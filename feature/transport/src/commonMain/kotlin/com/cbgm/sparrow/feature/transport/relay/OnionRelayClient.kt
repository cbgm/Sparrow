package com.cbgm.sparrow.feature.transport.relay

import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.discovery.PreferredNodeStore
import com.cbgm.sparrow.feature.transport.discovery.SignedNodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.SparrowNodeDescriptor
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** One relay of an onion route: [relayUrl] is its `POST /v1/relay`. */
internal class RelayHop(
    val nodeId: String,
    val relayUrl: String,
    val relayKey: ByteArray
)

/**
 * Picks the relays from the last verified node directory: the app's own node first (it knows the app's address
 * anyway), then another node, which sees the destination but not the address. With a single relaying node the route
 * has one hop; with none (first start, older servers) there is no route.
 */
internal class RelaySelector(
    private val cache: NodeDirectoryCache,
    private val json: Json,
    private val preferredNodeStore: PreferredNodeStore?,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds,
    private val random: Random = Random.Default
) {
    suspend fun route(excludedNodeIds: Set<String> = emptySet()): List<RelayHop> {
        val relays =
            cache.read()
                ?.let { cached -> runCatching { json.decodeFromString<SignedNodeDirectory>(cached.encodedDirectory) }.getOrNull() }
                ?.directory
                ?.nodes
                .orEmpty()
                .filter { node -> node.validUntilEpochMilliseconds > now() }
                .mapNotNull(SparrowNodeDescriptor::relayHop)
        val preferred = preferredNodeStore?.get()
        val first = relays.firstOrNull { hop -> hop.nodeId == preferred } ?: relays.randomOrNull(random) ?: return emptyList()
        val second = relays.filter { hop -> hop.nodeId != first.nodeId && hop.nodeId !in excludedNodeIds }.randomOrNull(random)
        return listOfNotNull(first, second)
    }
}

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
     * failed second relay is replaced once by another one.
     */
    suspend fun send(exit: RelayExit): RelayExitResponse? {
        val route = selector.route().takeIf(List<RelayHop>::isNotEmpty) ?: return null
        return runCatching { sendThrough(route, exit) }.getOrElse { failure ->
            if (failure is CancellationException) throw failure
            val retry = route.getOrNull(1)?.let { failed -> selector.route(excludedNodeIds = setOf(failed.nodeId)) }
            retry?.takeIf { candidate -> candidate.getOrNull(1) != null }?.let { candidate -> sendThrough(candidate, exit) }
                ?: throw failure
        }
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
            httpClient.post(route.first().relayUrl) {
                contentType(ContentType.Application.OctetStream)
                setBody(layer)
            }
        check(response.status == HttpStatusCode.OK) { "Relay route failed with ${response.status}" }
        val reply =
            replyKeys.fold(response.readRawBytes()) { sealed, replyKey ->
                checkNotNull(cipher.openReply(sealed, replyKey)) { "Relay reply could not be opened" }
            }
        return json.decodeFromString<RelayExitResponse>(reply.decodeToString())
    }

    private suspend fun seal(
        layer: RelayLayer,
        hop: RelayHop
    ): ByteArray = cipher.seal(json.encodeToString(layer).encodeToByteArray(), hop.relayKey, RELAY_CONTEXT).getOrThrow()
}

private fun SparrowNodeDescriptor.relayHop(): RelayHop? =
    relayKey?.let { key -> relayUrl(clientEndpoint)?.let { url -> RelayHop(nodeId, url, key) } }

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
