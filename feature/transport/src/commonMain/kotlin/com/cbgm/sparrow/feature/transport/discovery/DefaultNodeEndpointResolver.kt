package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

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
            config.trustedRegistryRootNodeId ?: cached?.trustedRootForSource()

        // Never keep selecting nodes from an endpoint removed from configuration.
        // Older cache entries do not record their source; preserve their short-lived
        // fallback until we can associate a verified response with an endpoint.
        val cachedSourceIsConfigured =
            cached?.sourceControlPlaneBaseUrl?.let { source ->
                controlPlaneConfiguration.endpoints.value.any { it.baseUrl == source } &&
                    (
                        source in controlPlaneConfiguration.manualBaseUrls.value ||
                            controlPlaneConfiguration.verifiedDirectoryRootId(source) == cached.trustedRootForSource()
                    )
            } ?: (
                controlPlaneConfiguration.manualBaseUrls.value.isNotEmpty() &&
                    controlPlaneConfiguration.directoryUrl.value == null
            )
        if (
            !forceRefresh && cachedSourceIsConfigured &&
            isReusable(cachedDirectory, trustedRootNodeId, currentTime)
        ) {
            return endpointSelector.select(checkNotNull(cachedDirectory), localRoutingId)
        }

        val selectedDirectory =
            fetchConfiguredDirectory(
                cached = cached,
                currentTime = currentTime
            ).getOrElse { remoteError ->
                if (!cachedSourceIsConfigured) throw remoteError
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
            val result =
                try {
                    // A dead registry must not consume the entire connection attempt
                    // when another Control Plane is reachable.
                    withTimeout(CONTROL_PLANE_FETCH_TIMEOUT_MILLISECONDS.milliseconds) {
                        fetchAndCache(
                            endpoint = endpoint,
                            cached = cached,
                            currentTime = currentTime
                        )
                    }
                } catch (error: TimeoutCancellationException) {
                    Result.failure(error)
                }
            result.onSuccess { directory ->
                controlPlaneStatusStore.markAvailable(endpoint)
                controlPlaneConfiguration.markActive(endpoint)
                return Result.success(directory)
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                controlPlaneStatusStore.markUnreachable(endpoint)
                lastError = error
                logger.debug {
                    "Control Plane ${endpoint.baseUrl} unavailable for node discovery: " +
                        (error.message ?: error::class.simpleName)
                }
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
            val rootNodeId = trustedRoot(endpoint, remoteDirectory, cached)
            verifier
                .verify(
                    signedDirectory = remoteDirectory,
                    trustedRootNodeId = rootNodeId,
                    supportedProtocolVersion = config.supportedProtocolVersion,
                    nowEpochMilliseconds = currentTime
                ).getOrThrow()
            cacheVerifiedDirectory(endpoint, remoteDirectory, rootNodeId, cached)
            remoteDirectory
        }

    private fun trustedRoot(
        endpoint: ControlPlaneEndpoint,
        remoteDirectory: SignedNodeDirectory,
        cached: CachedNodeDirectory?
    ): String {
        // A root pinned for Control Plane A must not be applied to Control Plane B.
        // The previous implementation used one cached root globally; switching to
        // another independently signed plane then failed until the app was reinstalled.
        val directoryRoot = controlPlaneConfiguration.verifiedDirectoryRootId(endpoint.baseUrl)
        // An explicit/manual CP keeps its existing trust behavior. Dynamically
        // discovered CPs MUST match the directory's signed root ID, not TOFU.
        if (endpoint.baseUrl !in controlPlaneConfiguration.manualBaseUrls.value &&
            endpoint.baseUrl in controlPlaneConfiguration.directoryBaseUrls.value &&
            controlPlaneConfiguration.directoryUrl.value != null
        ) {
            require(directoryRoot != null) { "Discovered Control Plane has no verified identity" }
            require(
                config.trustedRegistryRootNodeId == null ||
                    config.trustedRegistryRootNodeId == directoryRoot
            ) { "Control Plane identity conflicts with explicitly pinned root" }
            val remoteRoot = verifier.rootNodeId(remoteDirectory).getOrThrow()
            require(remoteRoot == directoryRoot) { "Discovered Control Plane signing root does not match directory" }
            val previousRoot = cached?.trustedRootFor(endpoint.baseUrl)
            require(previousRoot == null || previousRoot == directoryRoot) {
                "Previously pinned Control Plane identity differs from directory listing"
            }
            return directoryRoot
        }
        config.trustedRegistryRootNodeId?.let { return it }
        val previousRoot =
            cached?.trustedRootFor(endpoint.baseUrl)
                ?: cached?.takeIf { it.sourceControlPlaneBaseUrl == endpoint.baseUrl }
                    ?.trustedRootNodeId
        if (previousRoot != null) {
            // A manually configured HTTPS address is already authenticated by TLS.
            // In the default (TOFU) mode, a fresh installation at that same address
            // may legitimately have a new registry signing root. The replacement is
            // only persisted AFTER the complete new directory passes verification.
            // A hard-pinned root is handled above and can never be replaced here.
            val remoteRoot = verifier.rootNodeId(remoteDirectory).getOrThrow()
            if (previousRoot == remoteRoot) return previousRoot
            if (
                endpoint.baseUrl.startsWith("https://") &&
                endpoint.baseUrl in controlPlaneConfiguration.manualBaseUrls.value
            ) {
                logger.warn {
                    "Registry signing root changed at configured HTTPS Control Plane " +
                        "${endpoint.baseUrl}; verifying fresh directory and updating " +
                        "endpoint-scoped trust after successful verification"
                }
                return remoteRoot
            }
            return previousRoot
        }

        val remoteRoot = verifier.rootNodeId(remoteDirectory).getOrThrow()
        // Legacy caches lack source provenance. Keep their root when it matches,
        // but do not incorrectly bind A's root to a different Control Plane B.
        if (cached?.sourceControlPlaneBaseUrl == null &&
            cached?.trustedRootNodeId == remoteRoot
        ) {
            return cached.trustedRootNodeId
        }
        logger.warn {
            "Trusting registry root $remoteRoot for ${endpoint.baseUrl} on first use; " +
                "configure a trusted root for production"
        }
        return remoteRoot
    }

    private suspend fun cacheVerifiedDirectory(
        endpoint: ControlPlaneEndpoint,
        remoteDirectory: SignedNodeDirectory,
        rootNodeId: String,
        previous: CachedNodeDirectory?
    ) {
        runCatching {
            // Persist the endpoint/root binding ONLY after signature verification.
            // Carry forward the root of older source-aware cache records, too.
            val trustedRoots = previous?.trustedRootsByControlPlane.orEmpty().toMutableMap()
            previous?.sourceControlPlaneBaseUrl?.let { source ->
                // An explicitly removed/replaced server is deliberately not a
                // valid root source. Do not re-persist its former trust under
                // the synthetic cache-invalidation marker.
                if (!source.startsWith("removed:") && source !in trustedRoots) {
                    trustedRoots[source] = previous.trustedRootNodeId
                }
            }
            trustedRoots[endpoint.baseUrl] = rootNodeId
            cache.write(
                CachedNodeDirectory(
                    encodedDirectory = json.encodeToString(remoteDirectory),
                    trustedRootNodeId = rootNodeId,
                    sourceControlPlaneBaseUrl = endpoint.baseUrl,
                    trustedRootsByControlPlane = trustedRoots
                )
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            logger.error(error) { "Signed node directory could not be cached" }
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

    private companion object {
        const val CONTROL_PLANE_FETCH_TIMEOUT_MILLISECONDS = 5_000L
    }

    private fun CachedNodeDirectory.decode(): SignedNodeDirectory? =
        runCatching {
            json.decodeFromString<SignedNodeDirectory>(encodedDirectory)
        }.getOrNull()
}
