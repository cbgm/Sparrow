package com.cbgm.securechat.feature.transport.discovery

class FailedNodeTracker(
    private val cooldownMilliseconds: Long,
    private val now: () -> Long
) {
    private val failedAt = mutableMapOf<String, Long>()

    fun recordFailure(nodeId: String) {
        failedAt[nodeId] = now()
    }

    fun recordSuccess(nodeId: String) {
        failedAt.remove(nodeId)
    }

    fun available(endpoints: List<NodeEndpoint>): List<NodeEndpoint> {
        removeExpiredFailures()
        return endpoints.filterNot { endpoint -> endpoint.nodeId in failedAt }
    }

    fun unavailableNodeIds(endpoints: List<NodeEndpoint>): Set<String> {
        removeExpiredFailures()
        val endpointIds = endpoints.map(NodeEndpoint::nodeId).toSet()
        return failedAt.keys.filterTo(mutableSetOf()) { nodeId -> nodeId in endpointIds }
    }

    /**
     * Returns the least-recently failed endpoint as a half-open probe when every
     * discovered node is cooling down. This prevents a total client-side blackout
     * while still preferring non-failed nodes whenever one is available.
     */
    fun probeCandidate(endpoints: List<NodeEndpoint>): NodeEndpoint? {
        removeExpiredFailures()
        return endpoints
            .filter { endpoint -> endpoint.nodeId in failedAt }
            .minByOrNull { endpoint -> failedAt.getValue(endpoint.nodeId) }
    }

    fun cooldownUntilEpochMillisecondsByNodeId(
        endpoints: List<NodeEndpoint>
    ): Map<String, Long> {
        removeExpiredFailures()
        val endpointIds = endpoints.map(NodeEndpoint::nodeId).toSet()

        return failedAt
            .filterKeys { nodeId -> nodeId in endpointIds }
            .mapValues { (_, failedAtEpochMilliseconds) ->
                failedAtEpochMilliseconds + cooldownMilliseconds
            }
    }

    private fun removeExpiredFailures() {
        val currentTime = now()
        failedAt.entries.removeAll { (_, failedAtEpochMilliseconds) ->
            currentTime - failedAtEpochMilliseconds >= cooldownMilliseconds
        }
    }
}
