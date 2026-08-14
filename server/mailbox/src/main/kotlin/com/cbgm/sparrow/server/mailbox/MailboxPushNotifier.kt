package com.cbgm.sparrow.server.mailbox

import com.cbgm.sparrow.server.persistence.ControlPlaneEndpointPool
import com.cbgm.sparrow.server.persistence.ServiceEnvironment
import com.cbgm.sparrow.server.persistence.controlPlaneUrlsFromEnvironment
import com.cbgm.sparrow.server.security.InternalApiAuthentication
import com.cbgm.sparrow.server.security.NodeIdentityStore
import com.cbgm.sparrow.server.security.NodeRequestAuthentication
import com.cbgm.sparrow.server.security.NodeRequestHeaders
import com.cbgm.sparrow.server.security.NodeRequestSigner
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import java.nio.file.Path

class MailboxPushNotifier private constructor(
    private val httpClient: HttpClient?,
    private val endpointPool: ControlPlaneEndpointPool?,
    private val internalBaseUrl: String?,
    private val internalToken: String?,
    private val nodeSigner: NodeRequestSigner?
) : AutoCloseable {
    suspend fun notify(recipientId: String): Boolean {
        val client = httpClient ?: return false
        return when {
            nodeSigner != null && endpointPool != null ->
                notifyUsingNodeIdentity(client, endpointPool, recipientId)
            internalBaseUrl != null ->
                notifyUsingInternalToken(client, internalBaseUrl, recipientId)
            else -> false
        }
    }

    private suspend fun notifyUsingNodeIdentity(
        client: HttpClient,
        pool: ControlPlaneEndpointPool,
        recipientId: String
    ): Boolean {
        val path = "/v1/node-push/wake-ups/$recipientId"
        for (baseUrl in pool.ordered()) {
            try {
                val authentication = requireNotNull(nodeSigner).sign("POST", path, "")
                val response =
                    client.post(baseUrl.trimEnd('/') + path) {
                        nodeAuthentication(authentication)
                    }
                if (response.status.isSuccess()) {
                    pool.markAvailable(baseUrl)
                    return true
                }
                pool.markUnavailable(baseUrl)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                pool.markUnavailable(baseUrl)
            }
        }
        return false
    }

    private suspend fun notifyUsingInternalToken(
        client: HttpClient,
        pushBaseUrl: String,
        recipientId: String
    ): Boolean =
        client
            .post("${pushBaseUrl.trimEnd('/')}/internal/v1/wake-ups/$recipientId") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
            }.status
            .isSuccess()

    override fun close() {
        httpClient?.close()
    }

    companion object {
        fun fromEnvironment(): MailboxPushNotifier {
            val nodeApiUrl = System.getenv("PUSH_NODE_API_URL")?.takeIf(String::isNotBlank)
            val internalUrl = System.getenv("PUSH_INTERNAL_URL")?.takeIf(String::isNotBlank)
            val endpointPool =
                nodeApiUrl?.let {
                    ControlPlaneEndpointPool(
                        controlPlaneUrlsFromEnvironment(
                            legacyEnvironmentNames = listOf("PUSH_NODE_API_URL"),
                            defaultUrl = it
                        )
                    )
                }
            return MailboxPushNotifier(
                httpClient = if (endpointPool != null || internalUrl != null) HttpClient(CIO) else null,
                endpointPool = endpointPool,
                internalBaseUrl = internalUrl,
                internalToken =
                    ServiceEnvironment.secret("PUSH_INTERNAL_API_TOKEN")
                        ?: ServiceEnvironment.secret("INTERNAL_API_TOKEN"),
                nodeSigner = endpointPool?.let { createNodeSigner() }
            )
        }

        fun disabled() = MailboxPushNotifier(null, null, null, null, null)

        private fun createNodeSigner(): NodeRequestSigner {
            val identityPath =
                ServiceEnvironment.string(
                    "NODE_IDENTITY_PATH",
                    ".sparrow-server/node.identity"
                )
            val identity = NodeIdentityStore(Path.of(identityPath)).loadOrCreate()
            return NodeRequestSigner(identity)
        }
    }
}

private fun HttpRequestBuilder.nodeAuthentication(authentication: NodeRequestAuthentication) {
    header(NodeRequestHeaders.NODE_ID, authentication.nodeId)
    header(NodeRequestHeaders.TIMESTAMP, authentication.timestampEpochMilliseconds)
    header(NodeRequestHeaders.NONCE, authentication.nonce)
    header(NodeRequestHeaders.SIGNATURE, authentication.signature)
}
