package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.RELAY_CONTEXT
import com.cbgm.sparrow.server.protocol.RelayExit
import com.cbgm.sparrow.server.protocol.RelayExitResponse
import com.cbgm.sparrow.server.protocol.RelayForward
import com.cbgm.sparrow.server.protocol.RelayLayer
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ReplySealing
import com.cbgm.sparrow.server.security.X25519KeyPair
import com.cbgm.sparrow.server.security.isPublicUnicast
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.io.readByteArray
import java.net.InetAddress
import java.net.URI

/**
 * One hop of the app's onion route to its Control Planes. The relay opens only its own layer ([RelayLayer]): it
 * either passes the inner layer on to the next relay or, as the last hop, makes the request to the Control Plane. It
 * seals what comes back with the layer's reply key. It keeps and logs nothing about the request.
 */
internal class OnionRelay(
    private val keys: X25519KeyPair,
    private val httpClient: HttpClient,
    private val destinations: RelayDestinationPolicy
) {
    /** The sealed reply, or which part of the route failed ([RelayReply]). */
    suspend fun relay(sealedLayer: ByteArray): RelayReply {
        val layer =
            keys.open(sealedLayer, RELAY_CONTEXT)
                ?.let { plaintext -> runCatching { serverJson.decodeFromString<RelayLayer>(plaintext.decodeToString()) } }
                ?.getOrNull()
                ?.takeIf { candidate -> candidate.replyKey.size == ReplySealing.KEY_BYTES }
                ?: return RelayReply.Refused
        val nextRelay = layer.forward
        val destination = layer.exit
        return when {
            nextRelay != null && destination == null ->
                attempt(RelayReply.NextRelayFailed) { forward(nextRelay, layer.replyKey) }
            destination != null && nextRelay == null ->
                attempt(RelayReply.ControlPlaneFailed) { exit(destination, layer.replyKey) }
            else -> RelayReply.Refused
        }
    }

    private suspend fun attempt(
        failure: RelayReply,
        hop: suspend () -> RelayReply
    ): RelayReply =
        try {
            hop()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            failure
        }

    /** A next relay that could not reach the Control Plane passes that on, so the app does not blame a relay. */
    private suspend fun forward(
        forward: RelayForward,
        replyKey: ByteArray
    ): RelayReply {
        val target = destinations.nextRelay(forward.relayUrl) ?: return RelayReply.NextRelayFailed
        val response =
            httpClient.request(target.toString()) {
                method = HttpMethod.Post
                contentType(ContentType.Application.OctetStream)
                setBody(forward.layer)
            }
        return when (response.status) {
            HttpStatusCode.OK -> response.limitedBody()?.let { reply -> sealed(reply, replyKey) } ?: RelayReply.NextRelayFailed
            HttpStatusCode.GatewayTimeout -> RelayReply.ControlPlaneFailed
            else -> RelayReply.NextRelayFailed
        }
    }

    private suspend fun exit(
        exit: RelayExit,
        replyKey: ByteArray
    ): RelayReply {
        val target = destinations.controlPlane(exit) ?: return RelayReply.ControlPlaneFailed
        val response =
            httpClient.request(target.toString()) {
                method = HttpMethod.parse(exit.method)
                exit.body?.let { body ->
                    contentType(exit.contentType?.let(ContentType::parse) ?: ContentType.Application.Json)
                    setBody(body)
                }
            }
        val body = response.limitedBody() ?: return RelayReply.ControlPlaneFailed
        val reply =
            serverJson
                .encodeToString(RelayExitResponse(response.status.value, response.headers[HttpHeaders.ContentType], body))
                .encodeToByteArray()
        return sealed(reply, replyKey)
    }

    private fun sealed(
        reply: ByteArray,
        replyKey: ByteArray
    ): RelayReply = RelayReply.Sealed(ReplySealing.seal(reply, replyKey))

    private suspend fun HttpResponse.limitedBody(): ByteArray? =
        bodyAsChannel().readBuffer(MAXIMUM_BODY_BYTES + 1L).readByteArray().takeIf { it.size <= MAXIMUM_BODY_BYTES }

    fun close() {
        httpClient.close()
    }

    companion object {
        const val MAXIMUM_BODY_BYTES = 512 * 1024
    }
}

/** What a relay answers ([installRelayRoute] maps it to the HTTP status the app reads). */
internal sealed interface RelayReply {
    /** The reply, sealed with the layer's reply key: 200. */
    class Sealed(
        val bytes: ByteArray
    ) : RelayReply

    /** The layer is not for this relay (for example an outdated relay key) or not a valid layer: 400. */
    data object Refused : RelayReply

    /** The next relay did not pass the request on: 502. */
    data object NextRelayFailed : RelayReply

    /** As the last hop: the Control Plane could not be reached or the destination is not allowed: 504. */
    data object ControlPlaneFailed : RelayReply
}

/**
 * Where relays may send requests: only to another relay's `/v1/relay` and to the Control Plane paths the app uses,
 * never to private addresses unless [allowPrivateAddresses] (LAN installations). So the relay is no open proxy.
 */
internal class RelayDestinationPolicy(
    private val allowPrivateAddresses: Boolean,
    private val resolve: (String) -> List<InetAddress> = { host -> InetAddress.getAllByName(host).toList() }
) {
    fun nextRelay(url: String): URI? = target(url)?.takeIf { uri -> uri.rawPath == RELAY_PATH && uri.rawQuery == null }

    /**
     * A request the session node makes for the app through its circuit: only the node mailbox operations, waiting for
     * envelopes, and opening, sending, reading and deleting blobs in chunks (and reading ones sent in one piece).
     */
    fun sessionRequest(
        method: String,
        url: String
    ): URI? =
        target(url)?.takeIf { uri ->
            uri.rawQuery == null && SESSION_REQUESTS.any { (allowed, path) -> method == allowed && path.matches(uri.rawPath.orEmpty()) }
        }

    /** The next hop of a circuit: another node's `/v1/circuit` WebSocket, under the same address rules. */
    fun nextCircuitHop(url: String): URI? {
        val asHttp =
            when {
                url.startsWith("wss://") -> "https://" + url.removePrefix("wss://")
                url.startsWith("ws://") -> "http://" + url.removePrefix("ws://")
                else -> return null
            }
        return target(asHttp)?.takeIf { uri -> uri.rawPath == CIRCUIT_PATH && uri.rawQuery == null }?.let { URI(url) }
    }

    fun controlPlane(exit: RelayExit): URI? =
        target(exit.url)?.takeIf { uri ->
            CONTROL_PLANE_REQUESTS.any { (method, path) -> exit.method == method && path.matches(uri.rawPath.orEmpty()) }
        }

    private fun target(url: String): URI? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val host = uri.host?.takeIf(String::isNotBlank) ?: return null
        val schemeAllowed = uri.scheme == "https" || uri.scheme == "http"
        val addressesAllowed =
            allowPrivateAddresses ||
                runCatching { resolve(host) }.getOrDefault(emptyList()).let { addresses ->
                    addresses.isNotEmpty() && addresses.all(InetAddress::isPublicUnicast)
                }
        return uri.takeIf { schemeAllowed && uri.userInfo == null && uri.fragment == null && addressesAllowed }
    }

    private companion object {
        const val RELAY_PATH = "/v1/relay"
        const val CIRCUIT_PATH = "/v1/circuit"
        private const val ID = "[A-Za-z0-9_.-]{1,128}"
        private const val BLOB_ID = "[A-Za-z0-9_-]{16,128}"
        val SESSION_REQUESTS =
            listOf(
                "POST" to Regex("/v1/mailboxes"),
                "POST" to Regex("/v1/mailboxes/wait"),
                "POST" to Regex("/v1/mailboxes/$ID/envelopes"),
                "GET" to Regex("/v1/mailboxes/$ID/envelopes"),
                "DELETE" to Regex("/v1/mailboxes/$ID/envelopes/$ID"),
                "DELETE" to Regex("/v1/mailboxes/$ID"),
                "POST" to Regex("/v1/blobs"),
                "GET" to Regex("/v1/blobs/$BLOB_ID/chunks"),
                "PUT" to Regex("/v1/blobs/$BLOB_ID/chunks/[0-9]{1,6}"),
                "GET" to Regex("/v1/blobs/$BLOB_ID/chunks/[0-9]{1,6}"),
                // Blobs sent in one piece before chunks existed; goes once those have expired.
                "GET" to Regex("/v1/blobs/$BLOB_ID"),
                "DELETE" to Regex("/v1/blobs/$BLOB_ID")
            )
        val CONTROL_PLANE_REQUESTS =
            listOf(
                "GET" to Regex("/v1/nodes"),
                "GET" to Regex("/health/registry"),
                "POST" to Regex("/v1/alias-claims")
            )
    }
}

/** `POST /v1/relay`: the body is a sealed [RelayLayer], the answer the sealed reply. */
internal fun Route.installRelayRoute(relay: OnionRelay) {
    post("/v1/relay") {
        val sealedLayer = call.receiveChannel().readBuffer(OnionRelay.MAXIMUM_BODY_BYTES + 1L).readByteArray()
        val reply = sealedLayer.takeIf { it.size <= OnionRelay.MAXIMUM_BODY_BYTES }?.let { relay.relay(it) }
        when (reply) {
            is RelayReply.Sealed -> call.respondBytes(reply.bytes, ContentType.Application.OctetStream)
            null, RelayReply.Refused -> call.respond(HttpStatusCode.BadRequest)
            RelayReply.NextRelayFailed -> call.respond(HttpStatusCode.BadGateway)
            RelayReply.ControlPlaneFailed -> call.respond(HttpStatusCode.GatewayTimeout)
        }
    }
}

internal fun createRelayHttpClient(): HttpClient =
    HttpClient(CIO) {
        followRedirects = false
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = RELAY_TIMEOUT_MILLISECONDS
            connectTimeoutMillis = RELAY_TIMEOUT_MILLISECONDS
        }
    }

private const val RELAY_TIMEOUT_MILLISECONDS = 10_000L
