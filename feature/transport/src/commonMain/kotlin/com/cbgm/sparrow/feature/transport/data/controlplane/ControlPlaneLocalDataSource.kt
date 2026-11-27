package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpointStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The configured Control Planes, kept on the device, and how each answered last (in memory only). Everything in the
 * transport that talks to Control Planes reads them here.
 */
internal interface ControlPlaneLocalDataSource {
    val endpoints: StateFlow<List<ControlPlaneEndpoint>>
    val activeEndpoint: StateFlow<ControlPlaneEndpoint?>
    val manualBaseUrls: StateFlow<Set<String>>
    val directoryBaseUrls: StateFlow<Set<String>>
    val directoryUrl: StateFlow<String?>
    val statuses: StateFlow<List<ControlPlaneEndpointStatus>>

    /** Explicitly user-subscribed legacy JSON list, never a signed-directory trust source. */
    val jsonDirectoryUrl: StateFlow<String?>
        get() = MutableStateFlow(null)
    val jsonDirectoryBaseUrls: StateFlow<Set<String>>
        get() = MutableStateFlow(emptySet())

    /** Loads the saved configuration; until then everything is empty. */
    suspend fun initialize() = Unit

    suspend fun addManual(baseUrl: String): Result<Unit>

    suspend fun removeManual(baseUrl: String): Result<Unit>

    suspend fun setDirectoryUrl(url: String?): Result<Unit>

    /** Optional development bootstrap, ignored once the user has chosen/removed a directory. */
    suspend fun useDefaultDirectoryUrlIfUnconfigured(url: String): Result<Unit> = Result.success(Unit)

    suspend fun clearJsonDirectory(): Result<Unit> = Result.failure(UnsupportedOperationException("JSON directory is unavailable"))

    /** Explicit user removal, not a source URL edit: discard old JSON hints. */
    suspend fun clearJsonDirectoryCache(): Result<Unit> = Result.success(Unit)

    fun markAvailable(endpoint: ControlPlaneEndpoint)

    fun markUnreachable(endpoint: ControlPlaneEndpoint)

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
