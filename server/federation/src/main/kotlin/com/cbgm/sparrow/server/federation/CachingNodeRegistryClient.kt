package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.persistence.ControlPlaneEndpointPool
import com.cbgm.sparrow.server.protocol.NodeCapability
import com.cbgm.sparrow.server.protocol.SignedNodeDirectory
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor
import com.cbgm.sparrow.server.security.ProtocolSignatures
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

internal class CachingNodeRegistryClient(
    private val httpClient: HttpClient,
    private val endpointPool: ControlPlaneEndpointPool,
    private val refreshIntervalMilliseconds: Long = DEFAULT_REFRESH_INTERVAL_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) : NodeRegistryClient,
    PeerNodeDirectory {
    // Each Control Plane signs its own node directory. Do not let the first
    // reachable plane replace every other plane's nodes; preserve the last
    // verified, still-valid descriptors if one plane is temporarily offline.
    private val directories = ConcurrentHashMap<String, Map<String, SparrowNodeDescriptor>>()

    init {
        require(refreshIntervalMilliseconds > 0L)
    }

    override suspend fun find(nodeId: String): SparrowNodeDescriptor? {
        purgeExpiredAndRemoved()
        directories.values.asSequence()
            .mapNotNull { it[nodeId] }
            .filter(::isUsable)
            .firstOrNull()
            ?.let { return it }

        // A node may have registered since the previous 10s refresh.
        // Direct lookups are restricted to the *current* endpoint pool.
        return fetchDescriptor(nodeId)
    }

    override suspend fun peers(): List<SparrowNodeDescriptor> {
        purgeExpiredAndRemoved()
        return directories.values.asSequence()
            .flatMap { it.values.asSequence() }
            .filter(::isUsable)
            .filter { NodeCapability.FEDERATION in it.capabilities }
            .filter { SUPPORTED_PROTOCOL_VERSION in it.protocolVersions }
            .distinctBy(SparrowNodeDescriptor::nodeId)
            .sortedBy(SparrowNodeDescriptor::nodeId)
            .toList()
    }

    suspend fun runRefreshLoop() {
        while (true) {
            try {
                refresh()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // A directory outage must never stop the service or discard
                // previously verified and still-valid node descriptors.
            }
            delay(refreshIntervalMilliseconds.milliseconds)
        }
    }

    internal suspend fun refresh() {
        purgeExpiredAndRemoved()
        var lastError: Throwable? = null
        var anyRefreshed = false
        // Every plane owns its node registry; fetching only the first plane
        // would make remote nodes invisible even after live discovery.
        for (baseUrl in endpointPool.ordered()) {
            try {
                val directory = fetchDirectory(baseUrl)
                directories[baseUrl] = directory.directory.nodes
                    .filter(::isUsable)
                    .associateBy(SparrowNodeDescriptor::nodeId)
                endpointPool.markAvailable(baseUrl)
                anyRefreshed = true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                endpointPool.markUnavailable(baseUrl)
                lastError = error
            }
        }
        purgeExpiredAndRemoved()
        if (!anyRefreshed) {
            throw lastError ?: IllegalStateException("No control-plane registry is available")
        }
    }

    private suspend fun fetchDirectory(baseUrl: String): SignedNodeDirectory {
        val response = httpClient.get("${baseUrl.trimEnd('/')}/v1/nodes")
        check(response.status.isSuccess()) {
            "Node directory failed with HTTP ${response.status.value}"
        }
        return response.body<SignedNodeDirectory>().also { signedDirectory ->
            val expectedRoot = endpointPool.expectedDirectoryRootKey(baseUrl)
            if (expectedRoot != null) {
                // A valid self-signed directory is not sufficient for a newly
                // discovered plane. Bind its root certificate to the public
                // key in the directory worker's authenticated publication.
                val root = signedDirectory.authorityCertificate?.rootPublicKey
                    ?: signedDirectory.authorityPublicKey
                check(root.contentEquals(expectedRoot)) {
                    "Control Plane registry root does not match the discovered identity"
                }
            }
            check(ProtocolSignatures.verifyDirectory(signedDirectory)) {
                "Node directory signature is invalid"
            }
            check(signedDirectory.directory.validUntilEpochMilliseconds > now()) {
                "Node directory is expired"
            }
        }
    }

    private suspend fun fetchDescriptor(nodeId: String): SparrowNodeDescriptor? {
        for (baseUrl in endpointPool.ordered()) {
            try {
                // Do NOT trust an individually self-signed node descriptor from
                // a directory-discovered origin: validate the signed directory
                // and its root identity before using the contained descriptor.
                val directory = fetchDirectory(baseUrl)
                val nodes = directory.directory.nodes
                    .filter(::isUsable)
                    .associateBy(SparrowNodeDescriptor::nodeId)
                directories[baseUrl] = nodes
                endpointPool.markAvailable(baseUrl)
                nodes[nodeId]?.let { return it }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                endpointPool.markUnavailable(baseUrl)
            }
        }
        purgeExpiredAndRemoved()
        return null
    }

    private fun purgeExpiredAndRemoved() {
        // Authenticated removal of an endpoint removes its cached nodes on the
        // very next request, even if that endpoint is no longer reachable.
        val current = endpointPool.all().toSet()
        directories.keys.retainAll(current)
        directories.replaceAll { _, descriptors -> descriptors.filterValues(::isUsable) }
    }

    private fun isUsable(descriptor: SparrowNodeDescriptor): Boolean =
        descriptor.validUntilEpochMilliseconds > now() &&
            ProtocolSignatures.verifyDescriptor(descriptor)

    private companion object {
        const val DEFAULT_REFRESH_INTERVAL_MILLISECONDS = 10_000L
        const val SUPPORTED_PROTOCOL_VERSION = 1
    }
}
