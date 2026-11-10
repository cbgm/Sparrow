package com.cbgm.sparrow.feature.transport.relay

import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.discovery.PreferredNodeStore
import com.cbgm.sparrow.feature.transport.discovery.SignedNodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.SparrowNodeDescriptor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** One relay of an onion route: [relayUrl] is its `POST /v1/relay`, [circuitUrl] its `/v1/circuit` WebSocket. */
internal class RelayHop(
    val nodeId: String,
    val relayUrl: String,
    val circuitUrl: String,
    val relayKey: ByteArray
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
    private val preferredNodeStore: PreferredNodeStore?,
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
     * Up to [hopCount] distinct relays, the first as above; fewer when fewer nodes relay. The last hop, the session
     * node, is one not in [avoidLastHops] when there is such a node, so the app's circuits end at different nodes.
     */
    suspend fun circuitRoute(
        hopCount: Int,
        avoidLastHops: Set<String> = emptySet()
    ): List<RelayHop> {
        val relays =
            cache.read()
                ?.let { cached -> runCatching { json.decodeFromString<SignedNodeDirectory>(cached.encodedDirectory) }.getOrNull() }
                ?.directory
                ?.nodes
                .orEmpty()
                .filter { node -> node.validUntilEpochMilliseconds > now() }
                .mapNotNull(SparrowNodeDescriptor::relayHop)
        // When every relay failed recently, they are all tried again rather than none.
        val available = relays.filterNot { hop -> hop.nodeId in coolingDown() }.ifEmpty { relays }
        val own = connectedNodeId() ?: preferredNodeStore?.get()
        val first = available.firstOrNull { hop -> hop.nodeId == own } ?: available.randomOrNull(random) ?: return emptyList()
        val others = available.filter { hop -> hop.nodeId != first.nodeId }.shuffled(random)
        val chosen = others.take(hopCount - 1)
        val replacement = others.drop(chosen.size).firstOrNull { hop -> hop.nodeId !in avoidLastHops }
        val ordered =
            when {
                chosen.isEmpty() || chosen.last().nodeId !in avoidLastHops -> chosen
                replacement != null -> chosen.dropLast(1) + replacement
                // Every unused node is avoided: a chosen hop that is not goes last, when there is one.
                else -> chosen.partition { hop -> hop.nodeId in avoidLastHops }.let { (avoided, free) -> avoided + free }
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

/**
 * Sends a request through the relays from [RelaySelector], one sealed layer per relay ([RelayLayer]). The first relay
 * learns the app's address but not the destination, the last the destination but not the address; neither can read
 * the request or the answer.
 */
class OnionRelayClient internal constructor(
    private val selector: RelaySelector,
    private val cipher: SealedMessageCipher,
    private val httpClient: HttpClient,
    private val json: Json
) {
    private val logger = SparrowLog.withTag("OnionRelayClient")

    /**
     * Null when no node relays yet; throws when the route fails, so a request is never sent around the relays. A
     * failed relay is left out and the request is sent once more through the remaining ones, if any.
     */
    suspend fun send(exit: RelayExit): RelayExitResponse? {
        val route = selector.route().takeIf(List<RelayHop>::isNotEmpty) ?: return null
        return try {
            sendRecordingFailures(route, exit)
        } catch (failure: RelayHopFailedException) {
            val retry =
                selector.route().takeIf { candidate ->
                    candidate.isNotEmpty() && candidate.map(RelayHop::nodeId) != route.map(RelayHop::nodeId)
                } ?: throw failure
            sendRecordingFailures(retry, exit)
        }
    }

    private suspend fun sendRecordingFailures(
        route: List<RelayHop>,
        exit: RelayExit
    ): RelayExitResponse =
        try {
            sendThrough(route, exit)
        } catch (failure: RelayHopFailedException) {
            selector.recordFailure(failure.nodeId)
            logger.warn { "Relay ${failure.nodeId.take(8)} failed (${failure.message}); it is left out for a while" }
            throw failure
        }

    private suspend fun sendThrough(
        route: List<RelayHop>,
        exit: RelayExit
    ): RelayExitResponse {
        val replyKeys = route.map { cipher.newReplyKey() }
        var layer =
            seal(RelayLayer(replyKey = replyKeys.last(), exit = exit), route.last())
        for (index in route.lastIndex - 1 downTo 0) {
            val forward = RelayForward(relayUrl = route[index + 1].relayUrl, layer = layer)
            layer = seal(RelayLayer(replyKey = replyKeys[index], forward = forward), route[index])
        }
        logger.debug { "Control Plane request through ${route.size} relay(s): ${route.joinToString(" -> ") { it.nodeId.take(8) }}" }
        val response =
            try {
                httpClient.post(route.first().relayUrl) {
                    // The relay's status says which hop failed (failedHop), so it must not be thrown as an exception.
                    expectSuccess = false
                    contentType(ContentType.Application.OctetStream)
                    setBody(layer)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (unreachable: Exception) {
                throw RelayHopFailedException(route.first().nodeId, "not reachable: ${unreachable::class.simpleName}")
            }
        failedHop(route, response.status)?.let { failed -> throw RelayHopFailedException(failed.nodeId, "${response.status}") }
        check(response.status == HttpStatusCode.OK) { "Relay route failed with ${response.status}" }
        selector.recordSuccess(route)
        val reply =
            replyKeys.fold(response.readRawBytes()) { sealed, replyKey ->
                checkNotNull(cipher.openReply(sealed, replyKey)) { "Relay reply could not be opened" }
            }
        return json.decodeFromString<RelayExitResponse>(reply.decodeToString())
    }

    /**
     * The relay a failed answer points at: 400 means the first relay could not open its layer, 502 that the next relay
     * failed (older relays answer 502 for every failure). 504 means the Control Plane could not be reached, which no
     * relay is to blame for.
     */
    private fun failedHop(
        route: List<RelayHop>,
        status: HttpStatusCode
    ): RelayHop? =
        when (status) {
            HttpStatusCode.BadRequest -> route.first()
            HttpStatusCode.BadGateway -> route.getOrNull(1)
            else -> null
        }

    private suspend fun seal(
        layer: RelayLayer,
        hop: RelayHop
    ): ByteArray = cipher.seal(json.encodeToString(layer).encodeToByteArray(), hop.relayKey, RELAY_CONTEXT).getOrThrow()
}

private fun SparrowNodeDescriptor.relayHop(): RelayHop? {
    val key = relayKey ?: return null
    val relay = relayUrl(clientEndpoint) ?: return null
    val circuit = circuitUrl(clientEndpoint) ?: return null
    return RelayHop(nodeId, relay, circuit, key)
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
