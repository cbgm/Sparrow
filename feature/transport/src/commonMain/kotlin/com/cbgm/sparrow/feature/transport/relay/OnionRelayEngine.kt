package com.cbgm.sparrow.feature.transport.relay

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.json.Json

/**
 * Ktor engine for the app's requests to Control Planes: each goes through [OnionRelayClient], so no Control Plane
 * sees the app's address. Until a node relays (first start, older servers) requests go out directly; a request
 * [RelayRequirement.ROUTE_ONLY] then fails instead. A node directory from a Control Plane whose root is not pinned
 * yet is fetched directly too ([RelayRequirement.DIRECT]): through a relay the last relay could hand over a forged one.
 */
internal class OnionRelayEngine(
    private val onion: OnionRelayClient,
    private val directClient: HttpClient,
    private val requirement: suspend (Url) -> RelayRequirement
) : HttpClientEngineBase("sparrow-onion-relay") {
    override val config: HttpClientEngineConfig = HttpClientEngineConfig()

    @OptIn(InternalAPI::class)
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = callContext()
        val body =
            when (val content = data.body) {
                is OutgoingContent.ByteArrayContent -> content.bytes()
                is OutgoingContent.NoContent -> null
                else -> error("Control Plane requests need a byte body")
            }
        val exit =
            RelayExit(
                method = data.method.value,
                url = data.url.toString(),
                contentType = body?.let { data.body.contentType?.toString() ?: ContentType.Application.Json.toString() },
                body = body
            )
        val response =
            when (requirement(data.url)) {
                RelayRequirement.DIRECT -> direct(exit)
                RelayRequirement.ROUTE_PREFERRED -> onion.send(exit) ?: direct(exit)
                RelayRequirement.ROUTE_ONLY -> checkNotNull(onion.send(exit)) { "No relaying node known yet" }
            }
        return HttpResponseData(
            statusCode = HttpStatusCode.fromValue(response.status),
            requestTime = GMTDate(),
            headers = response.contentType?.let { type -> headersOf(HttpHeaders.ContentType, type) } ?: headersOf(),
            version = HttpProtocolVersion.HTTP_1_1,
            body = ByteReadChannel(response.body),
            callContext = callContext
        )
    }

    private suspend fun direct(exit: RelayExit): RelayExitResponse {
        val response =
            directClient.request(exit.url) {
                method = HttpMethod.parse(exit.method)
                exit.body?.let { bytes ->
                    contentType(exit.contentType?.let(ContentType::parse) ?: ContentType.Application.Json)
                    setBody(bytes)
                }
            }
        return RelayExitResponse(response.status.value, response.headers[HttpHeaders.ContentType], response.readRawBytes())
    }
}

internal enum class RelayRequirement {
    DIRECT,
    ROUTE_PREFERRED,
    ROUTE_ONLY
}

/** The Control Plane client: requests go through [engine]; failures surface like the shared client's ([expectSuccess]). */
internal fun createControlPlaneHttpClient(
    engine: OnionRelayEngine,
    json: Json
): HttpClient =
    HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
    }
