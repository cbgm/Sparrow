package com.cbgm.sparrow.feature.transport.domain.model

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
