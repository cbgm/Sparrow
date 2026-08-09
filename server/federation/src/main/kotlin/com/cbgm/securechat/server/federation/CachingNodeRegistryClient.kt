package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.persistence.ControlPlaneEndpointPool
import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.SignedNodeDirectory
import com.cbgm.securechat.server.security.ProtocolSignatures
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

internal class CachingNodeRegistryClient(
    private val httpClient: HttpClient,
    private val endpointPool: ControlPlaneEndpointPool,
    private val refreshIntervalMilliseconds: Long = DEFAULT_REFRESH_INTERVAL_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) : NodeRegistryClient,
    PeerNodeDirectory {
    private val descriptors = ConcurrentHashMap<String, SecureChatNodeDescriptor>()

    init {
        require(refreshIntervalMilliseconds > 0L)
    }

    override suspend fun find(nodeId: String): SecureChatNodeDescriptor? {
        descriptors[nodeId]?.takeIf(::isUsable)?.let { descriptor ->
            return descriptor
        }

        return fetchDescriptor(nodeId)?.also { descriptor ->
            descriptors[nodeId] = descriptor
        }
    }

    override suspend fun peers(): List<SecureChatNodeDescriptor> {
        purgeExpired()
        return descriptors
            .values
            .filter { descriptor -> NodeCapability.FEDERATION in descriptor.capabilities }
            .filter { descriptor -> SUPPORTED_PROTOCOL_VERSION in descriptor.protocolVersions }
            .sortedBy(SecureChatNodeDescriptor::nodeId)
    }

    suspend fun runRefreshLoop() {
        while (true) {
            runCatching { refresh() }
            delay(refreshIntervalMilliseconds)
        }
    }

    internal suspend fun refresh() {
        var lastError: Throwable? = null
        for (baseUrl in endpointPool.ordered()) {
            try {
                val directory = fetchDirectory(baseUrl)
                replaceCache(directory)
                endpointPool.markAvailable(baseUrl)
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                endpointPool.markUnavailable(baseUrl)
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("No control-plane registry is available")
    }

    private suspend fun fetchDirectory(baseUrl: String): SignedNodeDirectory {
        val response = httpClient.get("${baseUrl.trimEnd('/')}/v1/nodes")
        check(response.status.isSuccess()) {
            "Node directory failed with HTTP ${response.status.value}"
        }
        return response.body<SignedNodeDirectory>().also { signedDirectory ->
            check(ProtocolSignatures.verifyDirectory(signedDirectory)) {
                "Node directory signature is invalid"
            }
            check(signedDirectory.directory.validUntilEpochMilliseconds > now()) {
                "Node directory is expired"
            }
        }
    }

    private suspend fun fetchDescriptor(nodeId: String): SecureChatNodeDescriptor? {
        for (baseUrl in endpointPool.ordered()) {
            try {
                val response = httpClient.get("${baseUrl.trimEnd('/')}/v1/nodes/$nodeId")
                when {
                    response.status == HttpStatusCode.OK -> {
                        val descriptor = response.body<SecureChatNodeDescriptor>()
                        endpointPool.markAvailable(baseUrl)
                        return descriptor.takeIf(::isUsable)
                    }
                    response.status == HttpStatusCode.NotFound -> Unit
                    response.status.value >= SERVER_ERROR_STATUS_CODE ->
                        endpointPool.markUnavailable(baseUrl)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                endpointPool.markUnavailable(baseUrl)
            }
        }
        return null
    }

    private fun replaceCache(signedDirectory: SignedNodeDirectory) {
        val current =
            signedDirectory.directory.nodes
                .filter(::isUsable)
                .associateBy(SecureChatNodeDescriptor::nodeId)
        descriptors.keys.retainAll(current.keys)
        descriptors.putAll(current)
    }

    private fun purgeExpired() {
        descriptors.entries.removeIf { entry -> !isUsable(entry.value) }
    }

    private fun isUsable(descriptor: SecureChatNodeDescriptor): Boolean =
        descriptor.validUntilEpochMilliseconds > now() &&
            ProtocolSignatures.verifyDescriptor(descriptor)

    private companion object {
        const val DEFAULT_REFRESH_INTERVAL_MILLISECONDS = 10_000L
        const val SUPPORTED_PROTOCOL_VERSION = 1
        const val SERVER_ERROR_STATUS_CODE = 500
    }
}
