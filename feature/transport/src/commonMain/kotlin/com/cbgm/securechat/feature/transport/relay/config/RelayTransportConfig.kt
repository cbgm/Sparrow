package com.cbgm.securechat.feature.transport.relay.config

data class RelayTransportConfig(
    val httpBaseUrl: String,
    val nodeRegistryBaseUrl: String,
    val trustedRegistryAuthorityNodeId: String? = null,
    val supportedProtocolVersion: Int = 1,
    val directoryRefreshIntervalMilliseconds: Long = 60_000L,
    val cachedDirectoryGraceMilliseconds: Long = 5L * 60L * 1_000L,
    val failedNodeCooldownMilliseconds: Long = 30_000L,
    /**
     * Maximum wait for the relay to accept an envelope.
     */
    val acknowledgementTimeoutMilliseconds: Long = 15_000L
) {
    init {
        require(
            httpBaseUrl.startsWith(prefix = "http://") ||
                httpBaseUrl.startsWith(prefix = "https://")
        ) {
            "Relay HTTP base URL must use http:// or https://"
        }

        require(
            nodeRegistryBaseUrl.startsWith(prefix = "http://") ||
                nodeRegistryBaseUrl.startsWith(prefix = "https://")
        ) {
            "Node registry base URL must use http:// or https://"
        }

        require(trustedRegistryAuthorityNodeId == null || trustedRegistryAuthorityNodeId.isNotBlank()) {
            "Trusted registry authority node ID must not be blank"
        }

        require(supportedProtocolVersion > 0) {
            "Supported protocol version must be positive"
        }

        require(directoryRefreshIntervalMilliseconds > 0L) {
            "Directory refresh interval must be positive"
        }

        require(cachedDirectoryGraceMilliseconds >= 0L) {
            "Cached directory grace period must not be negative"
        }

        require(failedNodeCooldownMilliseconds > 0L) {
            "Failed-node cooldown must be positive"
        }

        require(acknowledgementTimeoutMilliseconds > 0L) {
            "Acknowledgement timeout must be positive"
        }
    }
}
