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
import io.ktor.utils.io.readRemaining
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
    /** The sealed reply, or null when the layer is not for this relay or the next hop failed. */
    suspend fun relay(sealedLayer: ByteArray): ByteArray? {
        val layer =
            keys.open(sealedLayer, RELAY_CONTEXT)
                ?.let { plaintext -> runCatching { serverJson.decodeFromString<RelayLayer>(plaintext.decodeToString()) } }
                ?.getOrNull()
                ?.takeIf { candidate -> candidate.replyKey.size == ReplySealing.KEY_BYTES }
                ?: return null
        val nextRelay = layer.forward
        val destination = layer.exit
        val reply =
            try {
                when {
                    nextRelay != null && destination == null -> forward(nextRelay)
                    destination != null && nextRelay == null -> exit(destination)
                    else -> null
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
        return reply?.let { bytes -> ReplySealing.seal(bytes, layer.replyKey) }
    }

    private suspend fun forward(forward: RelayForward): ByteArray? {
        val target = destinations.nextRelay(forward.relayUrl) ?: return null
        val response =
            httpClient.request(target.toString()) {
                method = HttpMethod.Post
                contentType(ContentType.Application.OctetStream)
                setBody(forward.layer)
            }
        return response.takeIf { it.status == HttpStatusCode.OK }?.limitedBody()
    }

    private suspend fun exit(exit: RelayExit): ByteArray? {
        val target = destinations.controlPlane(exit) ?: return null
        val response =
            httpClient.request(target.toString()) {
                method = HttpMethod.parse(exit.method)
                exit.body?.let { body ->
                    contentType(exit.contentType?.let(ContentType::parse) ?: ContentType.Application.Json)
                    setBody(body)
                }
            }
        val body = response.limitedBody() ?: return null
        return serverJson
            .encodeToString(RelayExitResponse(response.status.value, response.headers[HttpHeaders.ContentType], body))
            .encodeToByteArray()
    }

    private suspend fun HttpResponse.limitedBody(): ByteArray? =
        bodyAsChannel().readRemaining(MAXIMUM_BODY_BYTES + 1L).readByteArray().takeIf { it.size <= MAXIMUM_BODY_BYTES }

    fun close() {
        httpClient.close()
    }

    companion object {
        const val MAXIMUM_BODY_BYTES = 512 * 1024
    }
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
        val CONTROL_PLANE_REQUESTS =
            listOf(
                "GET" to Regex("/v1/nodes"),
                "GET" to Regex("/health/registry"),
                "POST" to Regex("/v1/alias-claims"),
                "POST" to Regex("/push/devices"),
                "GET" to Regex("/push/wake/[A-Za-z0-9_-]{1,128}/inbox"),
                "POST" to Regex("/push/wake/[A-Za-z0-9_-]{1,128}/inbox/[A-Za-z0-9_.-]{1,128}/ack")
            )
    }
}

/** `POST /v1/relay`: the body is a sealed [RelayLayer], the answer the sealed reply. */
internal fun Route.installRelayRoute(relay: OnionRelay) {
    post("/v1/relay") {
        val sealedLayer = call.receiveChannel().readRemaining(OnionRelay.MAXIMUM_BODY_BYTES + 1L).readByteArray()
        val reply = sealedLayer.takeIf { it.size <= OnionRelay.MAXIMUM_BODY_BYTES }?.let { relay.relay(it) }
        when (reply) {
            null -> call.respond(HttpStatusCode.BadGateway)
            else -> call.respondBytes(reply, ContentType.Application.OctetStream)
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
