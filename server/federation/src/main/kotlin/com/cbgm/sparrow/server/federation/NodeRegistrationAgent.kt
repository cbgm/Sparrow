package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.persistence.ControlPlaneEndpointPool
import com.cbgm.sparrow.server.protocol.NodeCapability
import com.cbgm.sparrow.server.protocol.NodeHeartbeatRequest
import com.cbgm.sparrow.server.protocol.NodeRegistrationRequest
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.protocol.unsigned
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.ProtocolSignatures
import com.cbgm.sparrow.server.security.Signatures
import com.cbgm.sparrow.server.security.relayKeyPair
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class NodeRegistrationAgent(
    private val httpClient: HttpClient,
    private val identity: NodeIdentity,
    private val config: NodeRegistrationConfig,
    private val endpointPool: ControlPlaneEndpointPool,
    private val now: () -> Long = System::currentTimeMillis
) {
    suspend fun run() {
        val registrationClient =
            NodeRegistrationClient(
                httpClient = httpClient,
                identity = identity,
                config = config,
                endpointPool = endpointPool,
                now = now
            )
        while (currentCoroutineContext().isActive) {
            registrationClient.synchronize()
            delay(config.heartbeatIntervalMilliseconds.milliseconds)
        }
    }
}

private class NodeRegistrationClient(
    private val httpClient: HttpClient,
    private val identity: NodeIdentity,
    private val config: NodeRegistrationConfig,
    private val endpointPool: ControlPlaneEndpointPool,
    private val now: () -> Long
) {
    private val nextRegistrationAt = mutableMapOf<String, Long>()
    private val retryAfter = mutableMapOf<String, Long>()

    suspend fun synchronize() {
        endpointPool.all().forEach { baseUrl -> synchronizeEndpoint(baseUrl) }
    }

    private suspend fun synchronizeEndpoint(baseUrl: String) {
        val currentTime = now()
        if (currentTime < (retryAfter[baseUrl] ?: Long.MIN_VALUE)) {
            return
        }

        try {
            if (currentTime >= (nextRegistrationAt[baseUrl] ?: Long.MIN_VALUE)) {
                register(baseUrl)
            }
            heartbeat(baseUrl)
            endpointPool.markReachable(baseUrl)
            retryAfter.remove(baseUrl)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            endpointPool.markUnavailable(baseUrl)
            val retryAt = now() + config.retryDelayMilliseconds
            nextRegistrationAt[baseUrl] = retryAt
            retryAfter[baseUrl] = retryAt
            logger.warn(
                "Control-plane synchronization failed for node {} at {}: {}",
                identity.nodeId,
                baseUrl,
                error.message ?: error::class.simpleName
            )
        }
    }

    private suspend fun register(baseUrl: String) {
        val response =
            httpClient.post("${baseUrl.trimEnd('/')}/v1/nodes") {
                contentType(ContentType.Application.Json)
                setBody(NodeRegistrationRequest(createDescriptor()))
            }
        if (response.status == HttpStatusCode.TooManyRequests) {
            nextRegistrationAt[baseUrl] = now() + config.retryDelayMilliseconds
            return
        }
        check(response.status.isSuccess()) {
            "Node registration failed with HTTP ${response.status.value}"
        }
        nextRegistrationAt[baseUrl] = now() + config.registrationRefreshMilliseconds
    }

    private suspend fun heartbeat(baseUrl: String) {
        val response =
            httpClient.post("${baseUrl.trimEnd('/')}/v1/nodes/${identity.nodeId}/heartbeat") {
                contentType(ContentType.Application.Json)
                setBody(createHeartbeat())
            }
        check(response.status.isSuccess()) {
            "Node heartbeat failed with HTTP ${response.status.value}"
        }
    }

    private fun createDescriptor(): SparrowNodeDescriptor =
        ProtocolSignatures.signDescriptor(
            SparrowNodeDescriptor(
                nodeId = identity.nodeId,
                clientEndpoint = config.clientEndpoint,
                mailboxEndpoint = config.mailboxEndpoint,
                identityPublicKey = identity.encodedPublicKey,
                protocolVersions = setOf(SUPPORTED_PROTOCOL_VERSION),
                capabilities = NodeCapability.entries.toSet(),
                validUntilEpochMilliseconds = now() + config.descriptorLifetimeMilliseconds,
                relayKey = identity.relayKeyPair().publicKey,
                operator = config.operator,
                signature = byteArrayOf()
            ),
            identity
        )

    /** Apps hold no sessions at a node any more, so the heartbeat reports no connection count. */
    private fun createHeartbeat(): NodeHeartbeatRequest {
        val unsigned =
            NodeHeartbeatRequest(
                nodeId = identity.nodeId,
                timestampEpochMilliseconds = now(),
                nonce = UUID.randomUUID().toString(),
                signature = byteArrayOf()
            )
        return unsigned.copy(
            signature =
                Signatures.sign(
                    serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                    identity.privateKey
                )
        )
    }

    private companion object {
        const val SUPPORTED_PROTOCOL_VERSION = 1
        val logger = LoggerFactory.getLogger(NodeRegistrationClient::class.java)
    }
}

data class NodeRegistrationConfig(
    val clientEndpoint: String,
    val mailboxEndpoint: String,
    /** Who runs this node, put into its descriptor; null when not declared. */
    val operator: String? = null,
    val descriptorLifetimeMilliseconds: Long = DEFAULT_DESCRIPTOR_LIFETIME_MILLISECONDS,
    val registrationRefreshMilliseconds: Long = DEFAULT_REGISTRATION_REFRESH_MILLISECONDS,
    val heartbeatIntervalMilliseconds: Long = DEFAULT_HEARTBEAT_INTERVAL_MILLISECONDS,
    val retryDelayMilliseconds: Long = DEFAULT_RETRY_DELAY_MILLISECONDS
) {
    init {
        require(descriptorLifetimeMilliseconds > registrationRefreshMilliseconds)
        require(registrationRefreshMilliseconds > heartbeatIntervalMilliseconds)
        require(heartbeatIntervalMilliseconds > 0L)
        require(retryDelayMilliseconds > 0L)
    }

    private companion object {
        const val DEFAULT_DESCRIPTOR_LIFETIME_MILLISECONDS = 60L * 60L * 1_000L
        const val DEFAULT_REGISTRATION_REFRESH_MILLISECONDS = 10L * 60L * 1_000L
        const val DEFAULT_HEARTBEAT_INTERVAL_MILLISECONDS = 1_000L
        const val DEFAULT_RETRY_DELAY_MILLISECONDS = 5_000L
    }
}
