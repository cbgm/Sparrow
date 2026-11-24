package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What the transport itself needs of the Control Plane configuration beyond what the user changes. */
internal interface ControlPlaneEndpointStore : ControlPlaneConfiguration {
    override val jsonDirectoryUrl: StateFlow<String?>
        get() = MutableStateFlow(null)
    override val jsonDirectoryBaseUrls: StateFlow<Set<String>>
        get() = MutableStateFlow(emptySet())

    override suspend fun initialize() = Unit

    override suspend fun useDefaultDirectoryUrlIfUnconfigured(url: String): Result<Unit> = Result.success(Unit)

    override suspend fun clearJsonDirectory(): Result<Unit> = Result.failure(UnsupportedOperationException("JSON directory is unavailable"))

    override suspend fun clearJsonDirectoryCache(): Result<Unit> = Result.success(Unit)

    fun orderedEndpoints(): List<ControlPlaneEndpoint>

    fun markActive(endpoint: ControlPlaneEndpoint)

    suspend fun replace(baseUrls: List<String>): Result<Unit>

    /** Import user-selected unsigned JSON addresses as removable manual entries, never as a directory. */
    suspend fun importManual(baseUrls: List<String>): Result<Unit> = Result.failure(UnsupportedOperationException("Manual JSON import is unavailable"))

    suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit>

    /** Legacy API: import addresses as removable manual entries; never replace the signed directory. */
    suspend fun replaceJsonDirectory(
        url: String,
        baseUrls: List<String>
    ): Result<Unit> = Result.failure(UnsupportedOperationException("JSON directory is unavailable"))

    /** Pinned root ID from an authenticated directory, never from URL-only hints. */
    fun verifiedDirectoryRootId(baseUrl: String): String? = null

    /** Apply an authenticated snapshot atomically with its URL-to-root bindings. */
    suspend fun replaceVerifiedDirectory(rootsByBaseUrl: Map<String, String>): Result<Unit> = replaceDirectory(rootsByBaseUrl.keys.toList())

    /**
     * Apply an authenticated snapshot while retaining previously discovered
     * entries that are still inside the directory's stale grace period.
     * Manual Control Planes are managed independently and are never part of
     * this stale set.
     */
    suspend fun replaceVerifiedDirectory(
        rootsByBaseUrl: Map<String, String>,
        staleBaseUrls: Set<String>
    ): Result<Unit> = replaceVerifiedDirectory(rootsByBaseUrl)

    suspend fun mergeDirectory(baseUrls: List<String>): Result<Unit> = replaceDirectory((directoryBaseUrls.value + baseUrls).toList())
}
