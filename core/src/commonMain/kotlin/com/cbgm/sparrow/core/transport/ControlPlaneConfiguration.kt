package com.cbgm.sparrow.core.transport

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ControlPlaneEndpoint(
    val baseUrl: String
) {
    init {
        require(
            baseUrl.startsWith(prefix = "http://") ||
                baseUrl.startsWith(prefix = "https://")
        ) {
            "Control-plane URL must use http:// or https://"
        }
    }
}

enum class ControlPlaneReachability {
    UNKNOWN,
    AVAILABLE,
    UNREACHABLE
}

data class ControlPlaneEndpointStatus(
    val endpoint: ControlPlaneEndpoint,
    val reachability: ControlPlaneReachability = ControlPlaneReachability.UNKNOWN,
    val isActive: Boolean = false
)

interface ControlPlaneConfiguration {
    val endpoints: StateFlow<List<ControlPlaneEndpoint>>
    val activeEndpoint: StateFlow<ControlPlaneEndpoint?>
    val manualBaseUrls: StateFlow<Set<String>>
    val directoryBaseUrls: StateFlow<Set<String>>
    val directoryUrl: StateFlow<String?>

    /** Explicitly user-subscribed legacy JSON list, never a signed-directory trust source. */
    val jsonDirectoryUrl: StateFlow<String?>
        get() = MutableStateFlow(null)
    val jsonDirectoryBaseUrls: StateFlow<Set<String>>
        get() = MutableStateFlow(emptySet())

    suspend fun initialize() = Unit

    fun orderedEndpoints(): List<ControlPlaneEndpoint>

    fun markActive(endpoint: ControlPlaneEndpoint)

    suspend fun replace(baseUrls: List<String>): Result<Unit>

    suspend fun addManual(baseUrl: String): Result<Unit>

    /** Import user-selected unsigned JSON addresses as removable manual entries, never as a directory. */
    suspend fun importManual(baseUrls: List<String>): Result<Unit> =
        Result.failure(UnsupportedOperationException("Manual JSON import is unavailable"))

    suspend fun removeManual(baseUrl: String): Result<Unit>

    suspend fun setDirectoryUrl(url: String?): Result<Unit>

    /** Optional development bootstrap, ignored once the user has chosen/removed a directory. */
    suspend fun useDefaultDirectoryUrlIfUnconfigured(url: String): Result<Unit> =
        Result.success(Unit)

    suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit>

    /** Legacy API: import addresses as removable manual entries; never replace the signed directory. */
    suspend fun replaceJsonDirectory(url: String, baseUrls: List<String>): Result<Unit> =
        Result.failure(UnsupportedOperationException("JSON directory is unavailable"))

    suspend fun clearJsonDirectory(): Result<Unit> =
        Result.failure(UnsupportedOperationException("JSON directory is unavailable"))

    /** Explicit user removal, not a source URL edit: discard old JSON hints. */
    suspend fun clearJsonDirectoryCache(): Result<Unit> = Result.success(Unit)

    /** Pinned root ID from an authenticated directory, never from URL-only hints. */
    fun verifiedDirectoryRootId(baseUrl: String): String? = null

    /** Apply an authenticated snapshot atomically with its URL-to-root bindings. */
    suspend fun replaceVerifiedDirectory(rootsByBaseUrl: Map<String, String>): Result<Unit> =
        replaceDirectory(rootsByBaseUrl.keys.toList())

    suspend fun mergeDirectory(baseUrls: List<String>): Result<Unit> =
        replaceDirectory((directoryBaseUrls.value + baseUrls).toList())
}

interface ControlPlaneStatusStore {
    val statuses: StateFlow<List<ControlPlaneEndpointStatus>>

    fun markAvailable(endpoint: ControlPlaneEndpoint)

    fun markUnreachable(endpoint: ControlPlaneEndpoint)
}

interface ControlPlaneHealthMonitor {
    suspend fun refresh()
}

interface ControlPlaneDirectorySynchronizer {
    /** Restore only a locally stored, cryptographically authenticated directory, without network I/O. */
    suspend fun restoreCached(): Result<Int> = Result.success(0)

    suspend fun refresh(): Result<Int>

    suspend fun synchronizeFrom(url: String): Result<Int>

    /** Import a separately configured, unsigned JSON feed on explicit user request. */
    suspend fun importJsonDirectory(url: String): Result<Int> =
        Result.failure(UnsupportedOperationException("JSON directory is unavailable"))

    /** Explicit directory removal only; no automatic reset after network/signature failures. */
    suspend fun forgetDirectoryTrustOnUserRemoval(): Result<Unit> = Result.success(Unit)
}
