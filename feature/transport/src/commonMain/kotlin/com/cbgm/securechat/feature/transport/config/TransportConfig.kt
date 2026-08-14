package com.cbgm.securechat.feature.transport.config

data class TransportConfig(
    val trustedRegistryRootNodeId: String? = null,
    val supportedProtocolVersion: Int = 1,
    val directoryRefreshIntervalMilliseconds: Long = 10_000L,
    val cachedDirectoryGraceMilliseconds: Long = 5L * 60L * 1_000L,
    val failedNodeCooldownMilliseconds: Long = 60_000L,
    /**
     * Maximum wait for the gateway to accept an envelope.
     */
    val acknowledgementTimeoutMilliseconds: Long = 15_000L
) {
    init {
        require(trustedRegistryRootNodeId == null || trustedRegistryRootNodeId.isNotBlank()) {
            "Trusted registry root node ID must not be blank"
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
