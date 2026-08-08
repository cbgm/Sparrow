package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.NodeRegistrationRequest
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.ProtocolSignatures
import com.cbgm.securechat.server.security.Signatures
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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
    private val now: () -> Long = System::currentTimeMillis
) {
    private val logger = LoggerFactory.getLogger(NodeRegistrationAgent::class.java)

    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            try {
                register()
                heartbeatUntilRefresh()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                logger.warn(
                    "Node registration loop failed for {}: {}",
                    identity.nodeId,
                    error.message ?: error::class.simpleName
                )
                delay(config.retryDelayMilliseconds.milliseconds)
            }
        }
    }

    private suspend fun register() {
        val validUntil = now() + config.descriptorLifetimeMilliseconds
        val descriptor =
            ProtocolSignatures.signDescriptor(
                SecureChatNodeDescriptor(
                    nodeId = identity.nodeId,
                    clientEndpoint = config.clientEndpoint,
                    federationEndpoint = config.federationEndpoint,
                    mailboxEndpoint = config.mailboxEndpoint,
                    identityPublicKey = identity.encodedPublicKey,
                    protocolVersions = setOf(1),
                    capabilities = NodeCapability.entries.toSet(),
                    validUntilEpochMilliseconds = validUntil,
                    signature = byteArrayOf()
                ),
                identity
            )
        val response =
            httpClient.post("${config.registryUrl.trimEnd('/')}/v1/nodes") {
                contentType(ContentType.Application.Json)
                setBody(NodeRegistrationRequest(descriptor))
            }

        check(response.status.isSuccess()) {
            "Node registration failed with HTTP ${response.status.value}"
        }
    }

    private suspend fun heartbeatUntilRefresh() {
        val refreshAt = now() + config.registrationRefreshMilliseconds
        while (currentCoroutineContext().isActive && now() < refreshAt) {
            delay(config.heartbeatIntervalMilliseconds.milliseconds)
            val timestamp = now()
            val unsigned =
                NodeHeartbeatRequest(
                    nodeId = identity.nodeId,
                    timestampEpochMilliseconds = timestamp,
                    nonce = UUID.randomUUID().toString(),
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

            check(response.status.isSuccess()) {
                "Node heartbeat failed with HTTP ${response.status.value}"
            }
        }
    }
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
    private companion object {
        const val DEFAULT_DESCRIPTOR_LIFETIME_MILLISECONDS = 10L * 60L * 1_000L
        const val DEFAULT_REGISTRATION_REFRESH_MILLISECONDS = 7L * 60L * 1_000L
        const val DEFAULT_HEARTBEAT_INTERVAL_MILLISECONDS = 30_000L
        const val DEFAULT_RETRY_DELAY_MILLISECONDS = 5_000L
    }
}
