package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.SignedNodeDirectory
import com.cbgm.securechat.server.security.ProtocolSignatures
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

internal class CachingNodeRegistryClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
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

        val remoteDescriptor = fetchDescriptor(nodeId)
        if (remoteDescriptor != null) {
            descriptors[nodeId] = remoteDescriptor
        }
        return remoteDescriptor
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
            runCatching {
                refresh()
            }
            delay(refreshIntervalMilliseconds)
        }
    }

    internal suspend fun refresh() {
        val signedDirectory =
            httpClient
                .get("${baseUrl.trimEnd('/')}/v1/nodes")
                .body<SignedNodeDirectory>()

        check(ProtocolSignatures.verifyDirectory(signedDirectory)) {
            "Node directory signature is invalid"
        }
        check(signedDirectory.directory.validUntilEpochMilliseconds > now()) {
            "Node directory is expired"
        }

        signedDirectory.directory.nodes
            .filter(::isUsable)
            .forEach { descriptor ->
                descriptors[descriptor.nodeId] = descriptor
            }

        purgeExpired()
    }

    private suspend fun fetchDescriptor(nodeId: String): SecureChatNodeDescriptor? =
        runCatching {
            val response = httpClient.get("${baseUrl.trimEnd('/')}/v1/nodes/$nodeId")
            if (response.status == HttpStatusCode.OK) {
                response.body<SecureChatNodeDescriptor>().takeIf(::isUsable)
            } else {
                null
            }
        }.getOrNull()

    private fun purgeExpired() {
        descriptors.entries.removeIf { entry -> !isUsable(entry.value) }
    }

    private fun isUsable(descriptor: SecureChatNodeDescriptor): Boolean =
        descriptor.validUntilEpochMilliseconds > now() &&
            ProtocolSignatures.verifyDescriptor(descriptor)

    private companion object {
        const val DEFAULT_REFRESH_INTERVAL_MILLISECONDS = 10_000L
        const val SUPPORTED_PROTOCOL_VERSION = 1
    }
}
