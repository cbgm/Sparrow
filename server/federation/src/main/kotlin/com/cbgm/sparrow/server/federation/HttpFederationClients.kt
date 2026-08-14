package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.persistence.ControlPlaneEndpointPool
import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedTypingEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.MailboxEnvelopeRequest
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.InternalApiAuthentication
import com.cbgm.sparrow.server.security.NodeRequestHeaders
import com.cbgm.sparrow.server.security.NodeRequestSigner
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class HttpPresenceDirectoryClient(
    private val httpClient: HttpClient,
    private val endpointPool: ControlPlaneEndpointPool
) : PresenceDirectoryClient {
    override suspend fun resolve(routingId: String): ClientRoutingResult {
        var emptyResult: ClientRoutingResult? = null
        for (baseUrl in endpointPool.ordered()) {
            try {
                val response = httpClient.get("${baseUrl.trimEnd('/')}/v1/routes/$routingId")
                if (!response.status.isSuccess()) {
                    if (response.status.value >= SERVER_ERROR_STATUS_CODE) {
                        endpointPool.markUnavailable(baseUrl)
                    }
                    continue
                }
                val result = response.body<ClientRoutingResult>()
                endpointPool.markAvailable(baseUrl)
                if (result.routes.isNotEmpty()) {
                    return result
                }
                emptyResult = result
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                endpointPool.markUnavailable(baseUrl)
            }
        }
        return emptyResult ?: ClientRoutingResult(routingId = routingId, routes = emptyList())
    }
}

class HttpLocalGatewayClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val internalToken: String?
) : LocalGatewayClient,
    LocalTypingGatewayClient,
    LocalRouteResolver {
    override suspend fun deliver(envelope: FederatedEnvelope): FederationAcknowledgement {
        val response =
            httpClient.post("$baseUrl/internal/v1/envelopes") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
                contentType(ContentType.Application.Json)
                setBody(envelope)
            }
        return if (response.status.isSuccess()) {
            response.body()
        } else {
            FederationAcknowledgement(
                envelopeId = envelope.envelopeId,
                state = EnvelopeAcceptanceState.QUEUED_AT_GATEWAY
            )
        }
    }

    override suspend fun deliver(event: FederatedTypingEvent): Boolean =
        httpClient
            .post("$baseUrl/internal/v1/typing-events") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
                contentType(ContentType.Application.Json)
                setBody(event)
            }.status
            .isSuccess()

    override suspend fun resolve(routingId: String): String? {
        val response =
            httpClient.get("$baseUrl/internal/v1/routes/$routingId") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
            }

        return if (response.status == HttpStatusCode.OK) {
            response.bodyAsText().takeIf(String::isNotBlank)
        } else {
            null
        }
    }
}

class HttpRemoteFederationClient(
    private val httpClient: HttpClient,
    private val signer: NodeRequestSigner
) : RemoteFederationClient,
    RemoteTypingFederationClient,
    RemoteRouteResolver {
    override suspend fun deliver(
        descriptor: SparrowNodeDescriptor,
        envelope: FederatedEnvelope
    ): FederationAcknowledgement {
        val path = "/v1/federation/envelopes"
        val body = serverJson.encodeToString(envelope)
        val authentication = signer.sign("POST", path, body)
        val response =
            httpClient.post(descriptor.federationEndpoint.trimEnd('/') + path) {
                header(NodeRequestHeaders.NODE_ID, authentication.nodeId)
                header(NodeRequestHeaders.TIMESTAMP, authentication.timestampEpochMilliseconds)
                header(NodeRequestHeaders.NONCE, authentication.nonce)
                header(NodeRequestHeaders.SIGNATURE, authentication.signature)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(body)
            }
        return if (response.status.isSuccess()) {
            response.body()
        } else {
            FederationAcknowledgement(
                envelopeId = envelope.envelopeId,
                state = EnvelopeAcceptanceState.QUEUED_AT_GATEWAY
            )
        }
    }

    override suspend fun deliver(
        descriptor: SparrowNodeDescriptor,
        event: FederatedTypingEvent
    ): Boolean {
        val path = "/v1/federation/typing-events"
        val body = serverJson.encodeToString(event)
        val authentication = signer.sign("POST", path, body)
        return httpClient
            .post(descriptor.federationEndpoint.trimEnd('/') + path) {
                header(NodeRequestHeaders.NODE_ID, authentication.nodeId)
                header(NodeRequestHeaders.TIMESTAMP, authentication.timestampEpochMilliseconds)
                header(NodeRequestHeaders.NONCE, authentication.nonce)
                header(NodeRequestHeaders.SIGNATURE, authentication.signature)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(body)
            }.status
            .isSuccess()
    }

    override suspend fun resolve(
        descriptor: SparrowNodeDescriptor,
        routingId: String
    ): String? {
        val path = "/v1/federation/routes/$routingId"
        val authentication = signer.sign("GET", path, "")
        val response =
            httpClient.get(descriptor.federationEndpoint.trimEnd('/') + path) {
                header(NodeRequestHeaders.NODE_ID, authentication.nodeId)
                header(NodeRequestHeaders.TIMESTAMP, authentication.timestampEpochMilliseconds)
                header(NodeRequestHeaders.NONCE, authentication.nonce)
                header(NodeRequestHeaders.SIGNATURE, authentication.signature)
            }

        return if (response.status == HttpStatusCode.OK) {
            response.bodyAsText().takeIf(String::isNotBlank)
        } else {
            null
        }
    }
}

class HttpMailboxClient(
    private val httpClient: HttpClient
) : MailboxClient {
    override suspend fun store(envelope: FederatedEnvelope): FederationAcknowledgement {
        val route = requireNotNull(envelope.mailboxRoute)
        return httpClient
            .post(
                route.nodeEndpoint.trimEnd('/') + "/v1/mailboxes/${route.mailboxId}/envelopes"
            ) {
                bearerAuth(route.sendCapability)
                contentType(ContentType.Application.Json)
                setBody(MailboxEnvelopeRequest(envelope))
            }.body()
    }
}

private const val SERVER_ERROR_STATUS_CODE = 500
