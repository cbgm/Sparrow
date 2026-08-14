package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class DefaultNodeEndpointResolver(
    private val source: NodeDirectorySource,
    private val json: Json,
    private val cache: NodeDirectoryCache,
    private val verifier: NodeDirectoryVerifier,
    private val config: TransportConfig,
    private val controlPlaneConfiguration: ControlPlaneConfiguration,
    private val controlPlaneStatusStore: ControlPlaneStatusStore,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) : NodeEndpointResolver {
    private val logger = SparrowLog.withTag("DefaultNodeEndpointResolver")
    private val resolutionMutex = Mutex()
    private var fetchedRemoteDirectory = false

    override suspend fun resolve(
        localRoutingId: String,
        forceRefresh: Boolean
    ): Result<List<NodeEndpoint>> =
        runCatching {
            require(localRoutingId.isNotBlank()) {
                "Local routing ID must not be blank"
            }

            resolutionMutex.withLock {
                resolveRegistry(
                    localRoutingId = localRoutingId,
                    forceRefresh = forceRefresh
                )
            }
        }

    private suspend fun resolveRegistry(
        localRoutingId: String,
        forceRefresh: Boolean
    ): List<NodeEndpoint> {
        val cached = cache.read()
        val cachedDirectory = cached?.decode()
        val currentTime = now()
        val trustedRootNodeId =
            config.trustedRegistryRootNodeId ?: cached?.trustedRootForSource()

        if (
            !forceRefresh &&
            fetchedRemoteDirectory &&
            isReusable(cachedDirectory, trustedRootNodeId, currentTime)
        ) {
            return checkNotNull(cachedDirectory).endpointsFor(localRoutingId)
        }

        val selectedDirectory =
            fetchConfiguredDirectory(
                cached = cached,
                currentTime = currentTime
            ).getOrElse { remoteError ->
                cachedFallback(
                    cachedDirectory = cachedDirectory,
                    trustedRootNodeId = trustedRootNodeId,
                    currentTime = currentTime,
                    remoteError = remoteError
                )
            }
        return selectedDirectory.endpointsFor(localRoutingId)
    }

    private suspend fun fetchConfiguredDirectory(
        cached: CachedNodeDirectory?,
        currentTime: Long
    ): Result<SignedNodeDirectory> {
        var lastError: Throwable? = null

        controlPlaneConfiguration.orderedEndpoints().forEach { endpoint ->
            fetchAndCache(
                endpoint = endpoint,
                cached = cached,
                currentTime = currentTime
            ).onSuccess { directory ->
                controlPlaneStatusStore.markAvailable(endpoint)
                controlPlaneConfiguration.markActive(endpoint)
                return Result.success(directory)
            }.onFailure { error ->
                controlPlaneStatusStore.markUnreachable(endpoint)
                lastError = error
            }
        }

        return Result.failure(
            lastError ?: IllegalStateException("No control plane is configured")
        )
    }

    private suspend fun isReusable(
        cachedDirectory: SignedNodeDirectory?,
        trustedRootNodeId: String?,
        currentTime: Long
    ): Boolean {
        if (cachedDirectory == null || trustedRootNodeId == null) {
            return false
        }
        val cacheAge = currentTime - cachedDirectory.directory.generatedAtEpochMilliseconds
        return cacheAge < config.directoryRefreshIntervalMilliseconds &&
            verifier
                .verify(
                    signedDirectory = cachedDirectory,
                    trustedRootNodeId = trustedRootNodeId,
                    supportedProtocolVersion = config.supportedProtocolVersion,
                    nowEpochMilliseconds = currentTime
                ).isSuccess
    }

    private suspend fun fetchAndCache(
        endpoint: ControlPlaneEndpoint,
        cached: CachedNodeDirectory?,
        currentTime: Long
    ): Result<SignedNodeDirectory> =
        source.fetch(endpoint.baseUrl).mapCatching { encodedDirectory ->
            val remoteDirectory = json.decodeFromString<SignedNodeDirectory>(encodedDirectory)
            val rootNodeId =
                trustedRoot(
                    endpoint = endpoint,
                    remoteDirectory = remoteDirectory,
                    cached = cached
                )
            verifier
                .verify(
                    signedDirectory = remoteDirectory,
                    trustedRootNodeId = rootNodeId,
                    supportedProtocolVersion = config.supportedProtocolVersion,
                    nowEpochMilliseconds = currentTime
                ).getOrThrow()
            cacheVerifiedDirectory(
                endpoint = endpoint,
                remoteDirectory = remoteDirectory,
                rootNodeId = rootNodeId,
                cached = cached
            )
            fetchedRemoteDirectory = true
            remoteDirectory
        }

    private fun trustedRoot(
        endpoint: ControlPlaneEndpoint,
        remoteDirectory: SignedNodeDirectory,
        cached: CachedNodeDirectory?
    ): String =
        config.trustedRegistryRootNodeId
            ?: cached?.trustedRootFor(endpoint.baseUrl)
            ?: verifier.rootNodeId(remoteDirectory).getOrThrow().also { rootNodeId ->
                logger.warn {
                    "Trusting registry root $rootNodeId on first use for ${endpoint.baseUrl}"
                }
            }

    private suspend fun cacheVerifiedDirectory(
        endpoint: ControlPlaneEndpoint,
        remoteDirectory: SignedNodeDirectory,
        rootNodeId: String,
        cached: CachedNodeDirectory?
    ) {
        runCatching {
            val trustedRoots =
                cached
                    ?.trustedRootsByControlPlane
                    .orEmpty() + (endpoint.baseUrl to rootNodeId)
            cache.write(
                CachedNodeDirectory(
                    encodedDirectory = json.encodeToString(remoteDirectory),
                    trustedRootNodeId = rootNodeId,
                    sourceControlPlaneBaseUrl = endpoint.baseUrl,
                    trustedRootsByControlPlane = trustedRoots
                )
            )
        }.onFailure { error ->
            logger.warn {
                "Signed node directory could not be cached: ${error.message ?: "unknown error"}"
            }
        }
    }

    private suspend fun cachedFallback(
        cachedDirectory: SignedNodeDirectory?,
        trustedRootNodeId: String?,
        currentTime: Long,
        remoteError: Throwable
    ): SignedNodeDirectory {
        val fallbackDirectory = cachedDirectory ?: throw remoteError
        val fallbackRootNodeId = trustedRootNodeId ?: throw remoteError
        val cacheExpiry =
            fallbackDirectory.directory.validUntilEpochMilliseconds +
                config.cachedDirectoryGraceMilliseconds

        verifier
            .verify(
                signedDirectory = fallbackDirectory,
                trustedRootNodeId = fallbackRootNodeId,
                supportedProtocolVersion = config.supportedProtocolVersion,
                nowEpochMilliseconds = currentTime,
                allowDirectoryExpiredUntilEpochMilliseconds = cacheExpiry,
                descriptorExpiryGraceMilliseconds = config.cachedDirectoryGraceMilliseconds
            ).getOrElse { throw remoteError }

        logger.warn {
            "All configured control planes are unavailable; using the last valid signed directory"
        }
        return fallbackDirectory
    }

    private fun CachedNodeDirectory.decode(): SignedNodeDirectory? =
        runCatching {
            json.decodeFromString<SignedNodeDirectory>(encodedDirectory)
        }.getOrNull()

    private fun stableSelectionScore(
        localRoutingId: String,
        nodeId: String
    ): ULong {
        var hash = FNV_OFFSET_BASIS
        "$localRoutingId:$nodeId".forEach { character ->
            hash = hash xor character.code.toULong()
            hash *= FNV_PRIME
        }
        return hash
    }

    private fun SignedNodeDirectory.endpointsFor(localRoutingId: String): List<NodeEndpoint> {
        val endpoints =
            directory.nodes
                .filter { descriptor ->
                    config.supportedProtocolVersion in descriptor.protocolVersions &&
                        NodeCapability.GATEWAY in descriptor.capabilities
                }.map { descriptor ->
                    NodeEndpoint(
                        nodeId = descriptor.nodeId,
                        websocketUrl = descriptor.clientEndpoint,
                        mailboxRouteEndpoint = descriptor.mailboxEndpoint,
                        mailboxAccessEndpoint =
                            descriptor.mailboxEndpoint.clientAccessibleFrom(descriptor.clientEndpoint),
                        activeConnections = descriptor.activeConnections ?: 0
                    )
                }.distinctBy(NodeEndpoint::nodeId)
                .sortedWith(
                    compareBy<NodeEndpoint>(NodeEndpoint::activeConnections)
                        .thenByDescending { endpoint ->
                            stableSelectionScore(
                                localRoutingId = localRoutingId,
                                nodeId = endpoint.nodeId
                            )
                        }
                )

        check(endpoints.isNotEmpty()) {
            "Node directory does not contain a compatible gateway"
        }

        return endpoints
    }

    private companion object {
        const val FNV_OFFSET_BASIS: ULong = 14_695_981_039_346_656_037uL
        const val FNV_PRIME: ULong = 1_099_511_628_211uL
    }
}

private fun String.clientAccessibleFrom(clientEndpoint: String): String {
    val internalHost = substringAfter("://").substringBefore('/').substringBefore(':')
    if (internalHost !in setOf("localhost", "mailbox", "mailbox-b")) return this
    val clientAuthority = clientEndpoint.substringAfter("://").substringBefore('/')
    val clientHost = clientAuthority.substringBefore(':')
    val gatewayPort = clientAuthority.substringAfter(':', "8094").toIntOrNull() ?: 8094
    val mailboxPort = gatewayPort - if (gatewayPort >= 8_200) 102 else 2
    return "http://$clientHost:$mailboxPort"
}
