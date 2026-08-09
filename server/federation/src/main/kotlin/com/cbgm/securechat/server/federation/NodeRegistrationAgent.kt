package com.cbgm.securechat.server.federation

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
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class NodeRegistrationAgent(
    private val httpClient: HttpClient,
    private val identity: NodeIdentity,
    private val config: NodeRegistrationConfig,
    private val loadProvider: GatewayLoadProvider = GatewayLoadProvider { 0 },
    private val now: () -> Long = System::currentTimeMillis
) {
    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            try {
                val refreshAt = establishRegistration()
                maintainRegistration(initialRefreshAt = refreshAt)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                logger.warn(
                    "Node registration loop failed for {}: {}",
                    identity.nodeId,
                    error.message ?: error::class.simpleName
                )
                delay(retryDelay(error).milliseconds)
            }
        }
    }

    private suspend fun establishRegistration(): Long =
        try {
            register()
            try {
                heartbeat()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                logger.warn(
                    "Initial node load heartbeat failed for {}: {}",
                    identity.nodeId,
                    error.message ?: error::class.simpleName
                )
            }
            now() + config.registrationRefreshMilliseconds
        } catch (error: NodeRegistrationHttpException) {
            if (error.statusCode != HttpStatusCode.TooManyRequests.value) {
                throw error
            }

            val heartbeatSucceeded =
                try {
                    heartbeat()
                    true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    logger.warn(
                        "Existing node registration heartbeat failed for {}: {}",
                        identity.nodeId,
                        error.message ?: error::class.simpleName
                    )
                    false
                }

            if (!heartbeatSucceeded) {
                throw error
            }

            logger.warn(
                "Node registration for {} is rate-limited; " +
                    "continuing with the existing healthy registration",
                identity.nodeId
            )
            now() + retryDelay(error)
        }

    private suspend fun maintainRegistration(initialRefreshAt: Long) {
        var refreshAt = initialRefreshAt

        while (currentCoroutineContext().isActive) {
            delay(config.heartbeatIntervalMilliseconds.milliseconds)
            heartbeat()

            if (now() >= refreshAt) {
                refreshAt = refreshDescriptor()
            }
        }
    }

    private suspend fun refreshDescriptor(): Long =
        try {
            register()
            now() + config.registrationRefreshMilliseconds
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            logger.warn(
                "Node descriptor refresh failed for {}: {}; heartbeats will continue",
                identity.nodeId,
                error.message ?: error::class.simpleName
            )
            now() + retryDelay(error)
        }

    private suspend fun register() {
        val descriptor =
            ProtocolSignatures.signDescriptor(
                SecureChatNodeDescriptor(
                    nodeId = identity.nodeId,
                    clientEndpoint = config.clientEndpoint,
                    federationEndpoint = config.federationEndpoint,
                    mailboxEndpoint = config.mailboxEndpoint,
                    identityPublicKey = identity.encodedPublicKey,
                    protocolVersions = setOf(SUPPORTED_PROTOCOL_VERSION),
                    capabilities = NodeCapability.entries.toSet(),
                    validUntilEpochMilliseconds =
                        now() + config.descriptorLifetimeMilliseconds,
                    signature = byteArrayOf()
                ),
                identity
            )

        val response =
            httpClient.post("${config.registryUrl.trimEnd('/')}/v1/nodes") {
                contentType(ContentType.Application.Json)
                setBody(NodeRegistrationRequest(descriptor))
            }

        response.requireSuccessful("Node registration")
    }

    private suspend fun heartbeat() {
        val timestamp = now()
        val activeConnections =
            runCatching {
                loadProvider.activeConnections().coerceAtLeast(0)
            }.getOrElse { error ->
                logger.warn(
                    "Gateway load lookup failed for {}: {}; preserving the last reported load",
                    identity.nodeId,
                    error.message ?: error::class.simpleName
                )
                null
            }

        val unsigned =
            NodeHeartbeatRequest(
                nodeId = identity.nodeId,
                timestampEpochMilliseconds = timestamp,
                nonce = UUID.randomUUID().toString(),
                activeConnections = activeConnections,
                signature = byteArrayOf()
            )
        val heartbeat =
            unsigned.copy(
                signature =
                    Signatures.sign(
                        serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                        identity.privateKey
                    )
            )
        val response =
            httpClient.post(
                "${config.registryUrl.trimEnd('/')}/v1/nodes/${identity.nodeId}/heartbeat"
            ) {
                contentType(ContentType.Application.Json)
                setBody(heartbeat)
            }

        response.requireSuccessful("Node heartbeat")
    }

    private fun retryDelay(error: Exception): Long =
        (error as? NodeRegistrationHttpException)
            ?.retryAfterMilliseconds
            ?.coerceAtLeast(config.retryDelayMilliseconds)
            ?: config.retryDelayMilliseconds

    private companion object {
        const val SUPPORTED_PROTOCOL_VERSION = 1

        val logger = LoggerFactory.getLogger(NodeRegistrationAgent::class.java)
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
    val registryUrl: String,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String,
    val descriptorLifetimeMilliseconds: Long = DEFAULT_DESCRIPTOR_LIFETIME_MILLISECONDS,
    val registrationRefreshMilliseconds: Long = DEFAULT_REGISTRATION_REFRESH_MILLISECONDS,
    val heartbeatIntervalMilliseconds: Long = DEFAULT_HEARTBEAT_INTERVAL_MILLISECONDS,
    val retryDelayMilliseconds: Long = DEFAULT_RETRY_DELAY_MILLISECONDS
) {
    init {
        require(descriptorLifetimeMilliseconds > registrationRefreshMilliseconds) {
            "Node descriptor lifetime must exceed its registration refresh interval"
        }
        require(registrationRefreshMilliseconds > heartbeatIntervalMilliseconds) {
            "Node registration refresh interval must exceed its heartbeat interval"
        }
        require(heartbeatIntervalMilliseconds > 0L) {
            "Node heartbeat interval must be positive"
        }
        require(retryDelayMilliseconds > 0L) {
            "Node registration retry delay must be positive"
        }
    }

    private companion object {
        const val DEFAULT_DESCRIPTOR_LIFETIME_MILLISECONDS = 60L * 60L * 1_000L
        const val DEFAULT_REGISTRATION_REFRESH_MILLISECONDS = 10L * 60L * 1_000L
        const val DEFAULT_HEARTBEAT_INTERVAL_MILLISECONDS = 5_000L
        const val DEFAULT_RETRY_DELAY_MILLISECONDS = 5_000L
    }
}

private fun HttpResponse.requireSuccessful(operation: String) {
    if (status.isSuccess()) {
        return
    }

    val retryAfterMilliseconds =
        headers[RETRY_AFTER_HEADER]
            ?.toLongOrNull()
            ?.times(MILLISECONDS_PER_SECOND)

    throw NodeRegistrationHttpException(
        message = "$operation failed with HTTP ${status.value}",
        statusCode = status.value,
        retryAfterMilliseconds = retryAfterMilliseconds
    )
}

private class NodeRegistrationHttpException(
    message: String,
    val statusCode: Int,
    val retryAfterMilliseconds: Long?
) : IllegalStateException(message)

private const val RETRY_AFTER_HEADER = "Retry-After"
private const val MILLISECONDS_PER_SECOND = 1_000L
