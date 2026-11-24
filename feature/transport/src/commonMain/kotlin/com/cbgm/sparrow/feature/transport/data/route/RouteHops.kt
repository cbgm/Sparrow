package com.cbgm.sparrow.feature.transport.data.route

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.data.directory.SignedNodeDirectoryDto
import com.cbgm.sparrow.feature.transport.data.directory.SparrowNodeDescriptorDto
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * One relay of an onion route: [relayUrl] is its `POST /v1/relay`, [circuitUrl] its `/v1/circuit` WebSocket. [operator]
 * is who runs it as declared in its descriptor, normalized; a node that declares none counts as its own operator.
 */
internal class RelayHop(
    val nodeId: String,
    val relayUrl: String,
    val circuitUrl: String,
    val relayKey: ByteArray,
    val operator: String = nodeId
)

/**
 * Picks the relays from the last verified node directory. The first hop is the node the app is connected through (it
 * knows the app's address anyway), before a connection the node the user chose, otherwise any node. The second hop is
 * another node, which sees the destination but not the address. A relay that failed is left out for
 * [failureCooldownMilliseconds] (in memory only). With a single reachable relaying node the route has one hop: the
 * Control Plane still does not see the app's address, but that node sees the destination. With none (first start,
 * older servers) there is no route.
 */
internal class RelaySelector(
    private val cache: NodeDirectoryCache,
    private val json: Json,
    private val preferences: TransportPreferencesRepository?,
    private val connectedNodeId: () -> String?,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds,
    private val random: Random = Random.Default,
    private val failureCooldownMilliseconds: Long = DEFAULT_FAILURE_COOLDOWN_MILLISECONDS
) {
    private val logger = SparrowLog.withTag("RelaySelector")
    private val failedAt = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val singleHop = MutableStateFlow(false)

    suspend fun route(): List<RelayHop> = circuitRoute(hopCount = 2).also(::reportHopCount)

    /**
     * Up to [hopCount] relays of different operators, the first as above; fewer when fewer operators run relaying
     * nodes, since two hops of one operator could match both ends. The last hop, the session node, belongs to an
     * operator not in [avoidLastOperators] when there is one, so the app's circuits end at different operators.
     */
    suspend fun circuitRoute(
        hopCount: Int,
        avoidLastOperators: Set<String> = emptySet()
    ): List<RelayHop> {
        val relays =
            cache.read()
                ?.let { cached -> runCatching { json.decodeFromString<SignedNodeDirectoryDto>(cached.encodedDirectory) }.getOrNull() }
                ?.directory
                ?.nodes
                .orEmpty()
                .filter { node -> node.validUntilEpochMilliseconds > now() }
                .mapNotNull(SparrowNodeDescriptorDto::relayHop)
        // When every relay failed recently, they are all tried again rather than none.
        val available = relays.filterNot { hop -> hop.nodeId in coolingDown() }.ifEmpty { relays }
        val own = connectedNodeId() ?: preferences?.observePreferredNode()?.first()
        val first = available.firstOrNull { hop -> hop.nodeId == own } ?: available.randomOrNull(random) ?: return emptyList()
        val others = available.filter { hop -> hop.operator != first.operator }.shuffled(random).distinctBy(RelayHop::operator)
        val chosen = others.take(hopCount - 1)
        val replacement = others.drop(chosen.size).firstOrNull { hop -> hop.operator !in avoidLastOperators }
        val ordered =
            when {
                chosen.isEmpty() || chosen.last().operator !in avoidLastOperators -> chosen
                replacement != null -> chosen.dropLast(1) + replacement
                // Every unused operator is avoided: a chosen hop that is not goes last, when there is one.
                else -> chosen.partition { hop -> hop.operator in avoidLastOperators }.let { (avoided, free) -> avoided + free }
            }
        return listOf(first) + ordered
    }

    fun recordFailure(nodeId: String) {
        failedAt.update { failures -> failures + (nodeId to now()) }
    }

    fun recordSuccess(route: List<RelayHop>) {
        failedAt.update { failures -> failures - route.map(RelayHop::nodeId).toSet() }
    }

    private fun coolingDown(): Set<String> {
        val currentTime = now()
        return failedAt.value.filterValues { failed -> currentTime - failed < failureCooldownMilliseconds }.keys
    }

    private fun reportHopCount(route: List<RelayHop>) {
        val oneHop = route.size == 1
        val wasOneHop = singleHop.getAndUpdate { oneHop }
        if (oneHop && !wasOneHop) {
            logger.warn {
                "Only one relaying node reachable: the Control Plane does not see this device's address, " +
                    "but node ${route.single().nodeId.take(8)} sees the destination"
            }
        }
    }

    private companion object {
        // Short, so a node that is back is used again soon; while it is down, one request per cooldown tries it and
        // is resent at once through the remaining relays.
        const val DEFAULT_FAILURE_COOLDOWN_MILLISECONDS = 20L * 1_000L
    }
}

/** A relay of the route did not pass the request on; [nodeId] is left out of the next routes for a while. */
internal class RelayHopFailedException(
    val nodeId: String,
    message: String
) : IllegalStateException(message)

private fun SparrowNodeDescriptorDto.relayHop(): RelayHop? {
    val key = relayKey ?: return null
    val relay = relayUrl(clientEndpoint) ?: return null
    val circuit = circuitUrl(clientEndpoint) ?: return null
    return RelayHop(nodeId, relay, circuit, key, operator?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: nodeId)
}

/** `wss://host/v1/gateway` → `wss://host/v1/circuit`. */
internal fun circuitUrl(clientEndpoint: String): String? {
    val scheme = clientEndpoint.substringBefore("://", missingDelimiterValue = "").takeIf { it == "wss" || it == "ws" }
    val authority = clientEndpoint.substringAfter("://").substringBefore('/').substringBefore('?')
    return scheme?.let { authority.takeIf(String::isNotBlank)?.let { host -> "$it://$host/v1/circuit" } }
}

/** `wss://host/v1/gateway` → `https://host/v1/relay`. */
internal fun relayUrl(clientEndpoint: String): String? {
    val scheme =
        when {
            clientEndpoint.startsWith("wss://") -> "https://"
            clientEndpoint.startsWith("ws://") -> "http://"
            else -> return null
        }
    val authority = clientEndpoint.substringAfter("://").substringBefore('/').substringBefore('?')
    return authority.takeIf(String::isNotBlank)?.let { host -> "$scheme$host/v1/relay" }
}
