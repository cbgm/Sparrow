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
    private val endpointSelector: NodeEndpointSelector,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) : NodeEndpointResolver {
    private val logger = SparrowLog.withTag("DefaultNodeEndpointResolver")
    private val resolutionMutex = Mutex()

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
            config.trustedRegistryRootNodeId ?: cached?.trustedRootNodeId

        if (
            !forceRefresh &&
            isReusable(cachedDirectory, trustedRootNodeId, currentTime)
        ) {
            return endpointSelector.select(checkNotNull(cachedDirectory), localRoutingId)
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
        return endpointSelector.select(selectedDirectory, localRoutingId)
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
}
