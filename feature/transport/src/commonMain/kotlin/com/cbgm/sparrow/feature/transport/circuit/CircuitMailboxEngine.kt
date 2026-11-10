package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.core.util.logging.SparrowLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Ktor engine for the app's mailbox requests: each is made by the session node at the end of the app's circuit, so
 * the mailbox node does not see the device's address. Without a circuit the request goes out directly when the user
 * allows it ([CircuitFallbackStore], the default) and waits for a circuit otherwise. A circuit through one hop only
 * counts when sending directly is allowed: its single node sees both the device's address and the mailbox.
 */
internal class CircuitMailboxEngine(
    private val circuits: CircuitRunner,
    private val fallback: CircuitFallbackStore,
    private val directClient: HttpClient,
    private val waitMilliseconds: Long = DEFAULT_WAIT_MILLISECONDS
) : HttpClientEngineBase("sparrow-circuit-mailbox") {
    override val config: HttpClientEngineConfig = HttpClientEngineConfig()
    private val logger = SparrowLog.withTag("CircuitMailboxEngine")

    @OptIn(InternalAPI::class)
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = callContext()
        val body =
            when (val content = data.body) {
                is OutgoingContent.ByteArrayContent -> content.bytes()
                is OutgoingContent.NoContent -> ByteArray(0)
                else -> error("Mailbox requests need a byte body")
            }
        val request =
            CircuitHttpRequest(
                method = data.method.value,
                url = data.url.toString(),
                bearerToken = data.headers[HttpHeaders.Authorization]?.removePrefix("Bearer "),
                contentType =
                    body.takeIf { it.isNotEmpty() }?.let { data.body.contentType?.toString() ?: ContentType.Application.Json.toString() },
                body = body
            )
        val response = throughCircuit(request, data.attributes.getOrNull(CircuitKey)) ?: direct(request)
        return HttpResponseData(
            statusCode = HttpStatusCode.fromValue(response.status),
            requestTime = GMTDate(),
            headers = response.contentType?.let { type -> headersOf(HttpHeaders.ContentType, type) } ?: headersOf(),
            version = HttpProtocolVersion.HTTP_1_1,
            body = ByteReadChannel(response.body),
            callContext = callContext
        )
    }

    /** Null when the request goes out directly instead: no circuit, or it failed, and the user allows that. */
    private suspend fun throughCircuit(
        request: CircuitHttpRequest,
        key: String?
    ): CircuitHttpResponse? {
        val sendsDirectly = fallback.sendsDirectly()
        val circuit = circuits.currentCircuit(key, minimumHopCount(sendsDirectly)) ?: waitUnlessDirect(sendsDirectly)
        if (circuit == null) {
            logDirect(request, "no circuit")
            return null
        }
        return try {
            circuit.request(request).also {
                logger.debug { "Mailbox ${request.method} through circuit (${circuit.nodeIds.size} hop(s))" }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (refused: CircuitRequestRefusedException) {
            throw refused
        } catch (failure: Exception) {
            // The runner notices the closed circuit and builds a new one.
            circuit.close()
            if (!sendsDirectly) throw failure
            logDirect(request, "circuit failed: ${failure.message}")
            null
        }
    }

    private suspend fun waitUnlessDirect(sendsDirectly: Boolean): Circuit? =
        if (sendsDirectly) {
            null
        } else {
            circuits.awaitCircuit(waitMilliseconds, CircuitHopCount.PROTECTING) ?: throw ProtectedRouteMissingException()
        }

    private fun minimumHopCount(sendsDirectly: Boolean): Int = if (sendsDirectly) 1 else CircuitHopCount.PROTECTING

    private fun logDirect(
        request: CircuitHttpRequest,
        reason: String
    ) {
        logger.warn { "Mailbox ${request.method} sent directly ($reason): the mailbox node sees this device's address" }
    }

    private suspend fun direct(request: CircuitHttpRequest): CircuitHttpResponse {
        val response =
            directClient.request(request.url) {
                method = HttpMethod.parse(request.method)
                request.bearerToken?.let { token -> header(HttpHeaders.Authorization, "Bearer $token") }
                request.body.takeIf { it.isNotEmpty() }?.let { bytes ->
                    contentType(request.contentType?.let(ContentType::parse) ?: ContentType.Application.Json)
                    setBody(bytes)
                }
            }
        return CircuitHttpResponse(response.status.value, response.headers[HttpHeaders.ContentType], response.readRawBytes())
    }

    private companion object {
        const val DEFAULT_WAIT_MILLISECONDS = 30_000L
    }
}

/** Which mailbox or contact a request is about: requests with the same key use the same circuit. */
internal val CircuitKey = AttributeKey<String>("SparrowCircuitKey")

/** The mailbox client: requests go through [engine]; status codes are checked by the caller, as with the shared client. */
internal fun createMailboxHttpClient(
    engine: CircuitMailboxEngine,
    json: Json
): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
    }
