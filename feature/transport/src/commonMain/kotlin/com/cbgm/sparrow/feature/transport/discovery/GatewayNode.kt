package com.cbgm.sparrow.feature.transport.discovery

/** A gateway from the verified node directory, as the user can choose it. */
data class GatewayNode(
    val nodeId: String,
    val address: String,
    val activeConnections: Int
) {
    /**
     * True when [query] is part of the address or the node ID, ignoring case. Automatic sslip.io
     * names carry the IP with dashes, so a dotted IP finds them too.
     */
    fun matches(query: String): Boolean {
        val normalized = query.trim().lowercase()
        val candidates = listOf(address.lowercase(), address.lowercase().replace('-', '.'), nodeId.lowercase())
        return normalized.isEmpty() || candidates.any { candidate -> normalized in candidate }
    }
}

/** The host (and a non-default port) of a gateway's WebSocket URL. */
internal fun NodeEndpoint.toGatewayNode(): GatewayNode =
    GatewayNode(
        nodeId = nodeId,
        address = websocketUrl.substringAfter("://").substringBefore('/'),
        activeConnections = activeConnections
    )
