package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.feature.transport.config.TransportConfig

class NodeEndpointSelector(
    private val config: TransportConfig
) {
    fun select(
        signedDirectory: SignedNodeDirectory,
        localRoutingId: String
    ): List<NodeEndpoint> {
        require(localRoutingId.isNotBlank()) {
            "Local routing ID must not be blank"
        }

        val endpoints =
            signedDirectory.directory.nodes
                .filter { descriptor ->
                    config.supportedProtocolVersion in descriptor.protocolVersions &&
                        NodeCapability.GATEWAY in descriptor.capabilities
                }.map { descriptor ->
                    NodeEndpoint(
                        nodeId = descriptor.nodeId,
                        websocketUrl = descriptor.clientEndpoint,
                        mailboxRouteEndpoint = descriptor.mailboxEndpoint,
                        mailboxAccessEndpoint =
                            descriptor.mailboxEndpoint.clientAccessibleFrom(descriptor.clientEndpoint),
                        activeConnections = descriptor.activeConnections ?: 0
                    )
                }.distinctBy(NodeEndpoint::nodeId)
                .sortedWith(
                    compareBy<NodeEndpoint>(NodeEndpoint::activeConnections)
                        .thenByDescending { endpoint ->
                            stableSelectionScore(
                                localRoutingId = localRoutingId,
                                nodeId = endpoint.nodeId
                            )
                        }
                )

        check(endpoints.isNotEmpty()) {
            "Node directory does not contain a compatible gateway"
        }

        return endpoints
    }

    private fun stableSelectionScore(
        localRoutingId: String,
        nodeId: String
    ): ULong {
        var hash = FNV_OFFSET_BASIS
        "$localRoutingId:$nodeId".forEach { character ->
            hash = hash xor character.code.toULong()
            hash *= FNV_PRIME
        }
        return hash
    }

    private companion object {
        const val FNV_OFFSET_BASIS: ULong = 14_695_981_039_346_656_037uL
        const val FNV_PRIME: ULong = 1_099_511_628_211uL
    }
}

private fun String.clientAccessibleFrom(clientEndpoint: String): String {
    val internalHost = substringAfter("://").substringBefore('/').substringBefore(':')
    if (internalHost !in setOf("localhost", "mailbox", "mailbox-b")) return this

    val clientAuthority = clientEndpoint.substringAfter("://").substringBefore('/')
    val clientHost = clientAuthority.substringBefore(':')
    val gatewayPort = clientAuthority.substringAfter(':', "8094").toIntOrNull() ?: 8094
    val mailboxPort = gatewayPort - if (gatewayPort >= 8_200) 102 else 2
    return "http://$clientHost:$mailboxPort"
}
