package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.persistence.ControlPlaneEndpointPool
import com.cbgm.securechat.server.protocol.GatewayLoad
import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.NodeRegistrationRequest
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned
import com.cbgm.securechat.server.security.InternalApiAuthentication
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.ProtocolSignatures
import com.cbgm.securechat.server.security.Signatures
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
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
    private val loadProvider: GatewayLoadProvider = GatewayLoadProvider { 0 },
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
            registrationClient.synchronize(resolveLoad())
            delay(config.heartbeatIntervalMilliseconds.milliseconds)
        }
    }

    private suspend fun resolveLoad(): Int? =
        try {
            loadProvider.activeConnections().coerceAtLeast(0)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            logger.warn(
                "Gateway load lookup failed for {}: {}; preserving the last reported load",
                identity.nodeId,
                error.message ?: error::class.simpleName
            )
            null
        }

    private companion object {
        val logger = LoggerFactory.getLogger(NodeRegistrationAgent::class.java)
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

    suspend fun synchronize(activeConnections: Int?) {
        endpointPool.all().forEach { baseUrl ->
            synchronizeEndpoint(baseUrl, activeConnections)
        }
    }

    private suspend fun synchronizeEndpoint(
        baseUrl: String,
        activeConnections: Int?
    ) {
        val currentTime = now()
        if (currentTime < (retryAfter[baseUrl] ?: Long.MIN_VALUE)) {
            return
        }

        try {
            if (currentTime >= (nextRegistrationAt[baseUrl] ?: Long.MIN_VALUE)) {
                register(baseUrl)
            }
            heartbeat(baseUrl, activeConnections)
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

    private suspend fun heartbeat(
        baseUrl: String,
        activeConnections: Int?
    ) {
        val response =
            httpClient.post("${baseUrl.trimEnd('/')}/v1/nodes/${identity.nodeId}/heartbeat") {
                contentType(ContentType.Application.Json)
                setBody(createHeartbeat(activeConnections))
            }
        check(response.status.isSuccess()) {
            "Node heartbeat failed with HTTP ${response.status.value}"
        }
    }

    private fun createDescriptor(): SecureChatNodeDescriptor =
        ProtocolSignatures.signDescriptor(
            SecureChatNodeDescriptor(
                nodeId = identity.nodeId,
                clientEndpoint = config.clientEndpoint,
                federationEndpoint = config.federationEndpoint,
                mailboxEndpoint = config.mailboxEndpoint,
                identityPublicKey = identity.encodedPublicKey,
                protocolVersions = setOf(SUPPORTED_PROTOCOL_VERSION),
                capabilities = NodeCapability.entries.toSet(),
                validUntilEpochMilliseconds = now() + config.descriptorLifetimeMilliseconds,
                signature = byteArrayOf()
            ),
            identity
        )

    private fun createHeartbeat(activeConnections: Int?): NodeHeartbeatRequest {
        val unsigned =
            NodeHeartbeatRequest(
                nodeId = identity.nodeId,
                timestampEpochMilliseconds = now(),
                nonce = UUID.randomUUID().toString(),
                activeConnections = activeConnections,
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

fun interface GatewayLoadProvider {
    suspend fun activeConnections(): Int
}

internal class HttpGatewayLoadProvider(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val internalToken: String?
) : GatewayLoadProvider {
    override suspend fun activeConnections(): Int =
        httpClient
            .get("${baseUrl.trimEnd('/')}/internal/v1/load") {
                internalToken?.let { token ->
                    header(InternalApiAuthentication.TOKEN_HEADER, token)
                }
            }.body<GatewayLoad>()
            .activeConnections
}

data class NodeRegistrationConfig(
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String,
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
