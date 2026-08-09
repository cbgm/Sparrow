package com.cbgm.securechat.feature.transport.discovery

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneEndpoint
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class DefaultNodeEndpointResolver(
    private val source: NodeDirectorySource,
    private val json: Json,
    private val cache: NodeDirectoryCache,
    private val verifier: NodeDirectoryVerifier,
    private val config: RelayTransportConfig,
    private val controlPlaneConfiguration: ControlPlaneConfiguration,
    private val controlPlaneStatusStore: ControlPlaneStatusStore,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) : NodeEndpointResolver {
    private val logger = SecureChatLog.withTag("DefaultNodeEndpointResolver")
    private val resolutionMutex = Mutex()
    private var fetchedRemoteDirectory = false

    override suspend fun resolve(
        localRelayId: String,
        forceRefresh: Boolean
    ): Result<List<NodeEndpoint>> =
        runCatching {
            require(localRelayId.isNotBlank()) {
                "Local relay ID must not be blank"
            }

            resolutionMutex.withLock {
                resolveRegistry(
                    localRelayId = localRelayId,
                    forceRefresh = forceRefresh
                )
            }
        }

    private suspend fun resolveRegistry(
        localRelayId: String,
        forceRefresh: Boolean
    ): List<NodeEndpoint> {
        val cached = cache.read()
        val cachedDirectory = cached?.decode()
        val currentTime = now()
        val trustedRootNodeId =
            config.trustedRegistryRootNodeId ?: cached?.trustedRootNodeId

        if (
            !forceRefresh &&
            fetchedRemoteDirectory &&
            isReusable(cachedDirectory, trustedRootNodeId, currentTime)
        ) {
            return checkNotNull(cachedDirectory).endpointsFor(localRelayId)
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
        return selectedDirectory.endpointsFor(localRelayId)
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
            val rootNodeId = trustedRoot(remoteDirectory, cached)
            verifier
                .verify(
                    signedDirectory = remoteDirectory,
                    trustedRootNodeId = rootNodeId,
                    supportedProtocolVersion = config.supportedProtocolVersion,
                    nowEpochMilliseconds = currentTime
                ).getOrThrow()
            cacheVerifiedDirectory(remoteDirectory, rootNodeId)
            fetchedRemoteDirectory = true
            remoteDirectory
        }

    private fun trustedRoot(
        remoteDirectory: SignedNodeDirectory,
        cached: CachedNodeDirectory?
    ): String =
        config.trustedRegistryRootNodeId
            ?: cached?.trustedRootNodeId
            ?: verifier.rootNodeId(remoteDirectory).getOrThrow().also { rootNodeId ->
                logger.warn {
                    "Trusting registry root $rootNodeId on first use; " +
                        "configure its root node ID for production"
                }
            }

    private suspend fun cacheVerifiedDirectory(
        remoteDirectory: SignedNodeDirectory,
        rootNodeId: String
    ) {
        runCatching {
            cache.write(
                CachedNodeDirectory(
                    encodedDirectory = json.encodeToString(remoteDirectory),
                    trustedRootNodeId = rootNodeId
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
        localRelayId: String,
        nodeId: String
    ): ULong {
        var hash = FNV_OFFSET_BASIS
        "$localRelayId:$nodeId".forEach { character ->
            hash = hash xor character.code.toULong()
            hash *= FNV_PRIME
        }
        return hash
    }

    private fun SignedNodeDirectory.endpointsFor(localRelayId: String): List<NodeEndpoint> {
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
                                localRelayId = localRelayId,
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
