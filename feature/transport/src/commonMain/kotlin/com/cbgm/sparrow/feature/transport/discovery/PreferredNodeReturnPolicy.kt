package com.cbgm.sparrow.feature.transport.discovery

/**
 * After a failover the connection stays on a fallback node. It goes back to the favourite once the
 * favourite is listed in the verified directory again and its failure cooldown is over.
 */
object PreferredNodeReturnPolicy {
    fun shouldReturn(
        preferredNodeId: String?,
        connectedNodeId: String,
        endpoints: List<NodeEndpoint>,
        coolingDownNodeIds: Set<String>
    ): Boolean =
        preferredNodeId != null &&
            preferredNodeId != connectedNodeId &&
            preferredNodeId !in coolingDownNodeIds &&
            endpoints.any { endpoint -> endpoint.nodeId == preferredNodeId }
}
