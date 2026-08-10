package com.cbgm.securechat.core.transport

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

    fun orderedEndpoints(): List<ControlPlaneEndpoint>

    fun markActive(endpoint: ControlPlaneEndpoint)

    suspend fun replace(baseUrls: List<String>): Result<Unit>

    suspend fun addManual(baseUrl: String): Result<Unit>

    suspend fun removeManual(baseUrl: String): Result<Unit>

    suspend fun setDirectoryUrl(url: String?): Result<Unit>

    suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit>

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
    suspend fun refresh(): Result<Int>
}
