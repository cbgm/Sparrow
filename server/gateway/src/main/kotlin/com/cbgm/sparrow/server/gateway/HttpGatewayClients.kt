package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.persistence.ControlPlaneEndpointPool
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedTypingEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.PendingTransportEnvelopesResponse
import com.cbgm.sparrow.server.protocol.TransportEnvelope
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.InternalApiAuthentication
import com.cbgm.sparrow.server.security.NodeRequestAuthentication
import com.cbgm.sparrow.server.security.NodeRequestHeaders
import com.cbgm.sparrow.server.security.NodeRequestSigner
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class HttpFederationClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val internalToken: String?
) : FederationClient {
    override suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement =
        httpClient
            .post("${baseUrl.trimEnd('/')}/internal/v1/outgoing-envelopes") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
                contentType(ContentType.Application.Json)
                setBody(envelope)
            }.body()

    override suspend fun routeTyping(event: FederatedTypingEvent): Boolean =
        httpClient
            .post("${baseUrl.trimEnd('/')}/internal/v1/outgoing-typing-events") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
                contentType(ContentType.Application.Json)
                setBody(event)
            }.status
            .isSuccess()

    override suspend fun markStored(envelopeId: String) {
        httpClient.post(
            "${baseUrl.trimEnd('/')}/internal/v1/outgoing-envelopes/$envelopeId/stored"
        ) {
            internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
        }
    }
}

class HttpPresenceClient(
    private val httpClient: HttpClient,
    private val endpointPool: ControlPlaneEndpointPool,
    private val signer: NodeRequestSigner
) : PresenceClient {
    override suspend fun register(registration: ClientRouteRegistration): Boolean {
        var accepted = false
        endpointPool.all().forEach { baseUrl ->
            accepted = registerAt(baseUrl, registration) || accepted
        }
        return accepted
    }

    override suspend fun remove(
        routingId: String,
        connectionId: String
    ) {
        endpointPool.all().forEach { baseUrl ->
            try {
                removeAt(baseUrl, routingId, connectionId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                endpointPool.markUnavailable(baseUrl)
            }
        }
    }

    private suspend fun registerAt(
        baseUrl: String,
        registration: ClientRouteRegistration
    ): Boolean =
        try {
            val path = "/v1/routes/${registration.route.routingId}"
            val body = serverJson.encodeToString(registration)
            val authentication = signer.sign("PUT", path, body)
            val status =
                httpClient
                    .put(baseUrl.trimEnd('/') + path) {
                        nodeAuthentication(authentication)
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }.status
            if (status.isSuccess()) {
                endpointPool.markReachable(baseUrl)
                true
            } else {
                if (status.value >= SERVER_ERROR_STATUS_CODE || status == HttpStatusCode.TooManyRequests) {
                    endpointPool.markUnavailable(baseUrl)
                }
                false
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            endpointPool.markUnavailable(baseUrl)
            false
        }

    private suspend fun removeAt(
        baseUrl: String,
        routingId: String,
        connectionId: String
    ) {
        val path = "/v1/routes/$routingId/$connectionId"
        val authentication = signer.sign("DELETE", path, "")
        val response =
            httpClient.delete(baseUrl.trimEnd('/') + path) {
                nodeAuthentication(authentication)
            }
        if (response.status.value >= SERVER_ERROR_STATUS_CODE) {
            error("Presence removal failed with HTTP ${response.status.value}")
        }
        endpointPool.markReachable(baseUrl)
    }
}

class HttpLegacyPushClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val internalToken: String?
) : LegacyPushClient {
    override suspend fun store(envelope: TransportEnvelope): Boolean =
        httpClient
            .post("${baseUrl.trimEnd('/')}/internal/v1/envelopes") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
                contentType(ContentType.Application.Json)
                setBody(envelope)
            }.status
            .isSuccess()

    override suspend fun pending(recipientId: String): List<TransportEnvelope> =
        httpClient
            .get("${baseUrl.trimEnd('/')}/internal/v1/recipients/$recipientId/envelopes") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
            }.body<PendingTransportEnvelopesResponse>()
            .envelopes

    override suspend fun acknowledge(
        recipientId: String,
        envelopeId: String
    ) {
        httpClient.post(
            "${baseUrl.trimEnd('/')}/internal/v1/recipients/$recipientId/envelopes/$envelopeId/ack"
        ) {
            internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
        }
    }
}

class HttpNodePushClient(
    private val httpClient: HttpClient,
    private val endpointPool: ControlPlaneEndpointPool,
    private val signer: NodeRequestSigner
) : LegacyPushClient {
    override suspend fun store(envelope: TransportEnvelope): Boolean {
        val body = serverJson.encodeToString(envelope)
        val primary = storePrimary(body) ?: return false
        replicateToAvailableControlPlanes(
            body = body,
            primary = primary
        )
        return true
    }

    override suspend fun pending(recipientId: String): List<TransportEnvelope> {
        val path = "/v1/node-push/recipients/$recipientId/envelopes"
        val snapshots = mutableMapOf<String, List<TransportEnvelope>>()
        var lastError: Throwable? = null

        val candidates = endpointPool.availableEndpoints().ifEmpty { endpointPool.ordered() }
        candidates.forEach { baseUrl ->
            try {
                val response =
                    httpClient.get(baseUrl.trimEnd('/') + path) {
                        nodeAuthentication(signer.sign("GET", path, ""))
                    }
                if (response.status.isSuccess()) {
                    endpointPool.markReachable(baseUrl)
                    snapshots[baseUrl] = response.body<PendingTransportEnvelopesResponse>().envelopes
                } else {
                    handlePushFailure(baseUrl, response.status.value)
                    lastError =
                        IllegalStateException(
                            "Push pending failed with HTTP ${response.status.value}"
                        )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                endpointPool.markUnavailable(baseUrl)
                lastError = error
            }
        }

        if (snapshots.isEmpty()) {
            throw lastError ?: IllegalStateException("No control-plane push service is available")
        }

        val merged = mergePendingEnvelopes(snapshots.values.flatten())
        healPendingReplicas(merged, snapshots)
        return merged
    }

    override suspend fun acknowledge(
        recipientId: String,
        envelopeId: String
    ) {
        val path = "/v1/node-push/recipients/$recipientId/envelopes/$envelopeId/ack"
        val candidates = endpointPool.availableEndpoints().ifEmpty { endpointPool.ordered() }
        candidates.forEach { baseUrl ->
            try {
                val response =
                    httpClient.post(baseUrl.trimEnd('/') + path) {
                        nodeAuthentication(signer.sign("POST", path, ""))
                    }
                if (response.status.isSuccess()) {
                    endpointPool.markReachable(baseUrl)
                } else {
                    handlePushFailure(baseUrl, response.status.value)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                endpointPool.markUnavailable(baseUrl)
            }
        }
    }

    private suspend fun storePrimary(body: String): String? {
        for (baseUrl in endpointPool.ordered()) {
            try {
                val response = postEnvelope(baseUrl, NODE_ENVELOPE_PATH, body)
                if (response.status.isSuccess()) {
                    endpointPool.markAvailable(baseUrl)
                    return baseUrl
                }
                handlePushFailure(baseUrl, response.status.value)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                endpointPool.markUnavailable(baseUrl)
            }
        }
        return null
    }

    private suspend fun replicateToAvailableControlPlanes(
        body: String,
        primary: String
    ) {
        endpointPool
            .availableEndpoints()
            .filterNot { baseUrl -> baseUrl == primary }
            .forEach { baseUrl ->
                replicateEnvelope(baseUrl, body)
            }
    }

    private suspend fun healPendingReplicas(
        merged: List<TransportEnvelope>,
        snapshots: Map<String, List<TransportEnvelope>>
    ) {
        snapshots.forEach { (baseUrl, localEnvelopes) ->
            val localIds = localEnvelopes.mapTo(mutableSetOf(), TransportEnvelope::envelopeId)
            merged
                .filterNot { envelope -> envelope.envelopeId in localIds }
                .forEach { envelope ->
                    replicateEnvelope(baseUrl, serverJson.encodeToString(envelope))
                }
        }
    }

    private suspend fun replicateEnvelope(
        baseUrl: String,
        body: String
    ) {
        try {
            val response = postEnvelope(baseUrl, NODE_ENVELOPE_REPLICA_PATH, body)
            if (response.status.isSuccess()) {
                endpointPool.markReachable(baseUrl)
            } else {
                handlePushFailure(baseUrl, response.status.value)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            endpointPool.markUnavailable(baseUrl)
        }
    }

    private suspend fun postEnvelope(
        baseUrl: String,
        path: String,
        body: String
    ) =
        httpClient.post(baseUrl.trimEnd('/') + path) {
            nodeAuthentication(signer.sign("POST", path, body))
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private fun handlePushFailure(
        baseUrl: String,
        statusCode: Int
    ) {
        if (statusCode >= SERVER_ERROR_STATUS_CODE) {
            endpointPool.markUnavailable(baseUrl)
        }
    }

    private fun mergePendingEnvelopes(envelopes: List<TransportEnvelope>): List<TransportEnvelope> =
        envelopes
            .associateBy(TransportEnvelope::envelopeId)
            .values
            .sortedWith(
                compareBy(TransportEnvelope::createdAtEpochMilliseconds)
                    .thenBy(TransportEnvelope::envelopeId)
            )

    private companion object {
        const val NODE_ENVELOPE_PATH = "/v1/node-push/envelopes"
        const val NODE_ENVELOPE_REPLICA_PATH = "/v1/node-push/replicas/envelopes"
    }
}

private fun HttpRequestBuilder.nodeAuthentication(authentication: NodeRequestAuthentication) {
    header(NodeRequestHeaders.NODE_ID, authentication.nodeId)
    header(NodeRequestHeaders.TIMESTAMP, authentication.timestampEpochMilliseconds)
    header(NodeRequestHeaders.NONCE, authentication.nonce)
    header(NodeRequestHeaders.SIGNATURE, authentication.signature)
}

private const val SERVER_ERROR_STATUS_CODE = 500
