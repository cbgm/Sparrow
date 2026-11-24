package com.cbgm.sparrow.feature.transport.data.route.relay

import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.data.route.RelayHop
import com.cbgm.sparrow.feature.transport.data.route.RelayHopFailedException
import com.cbgm.sparrow.feature.transport.data.route.RelaySelector
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Sends a request through the relays from [RelaySelector], one sealed layer per relay ([RelayLayer]). The first relay
 * learns the app's address but not the destination, the last the destination but not the address; neither can read
 * the request or the answer.
 */
internal class OnionRelayClient internal constructor(
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
