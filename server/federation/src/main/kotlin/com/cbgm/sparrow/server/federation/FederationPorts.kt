package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedIndicatorEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor

fun interface PresenceDirectoryClient {
    suspend fun resolve(routingId: String): ClientRoutingResult
}

fun interface NodeRegistryClient {
    suspend fun find(nodeId: String): SparrowNodeDescriptor?
}

fun interface PeerNodeDirectory {
    suspend fun peers(): List<SparrowNodeDescriptor>
}

fun interface LocalGatewayClient {
    suspend fun deliver(envelope: FederatedEnvelope): FederationAcknowledgement
}

fun interface LocalIndicatorGatewayClient {
    suspend fun deliver(event: FederatedIndicatorEvent): Boolean
}

fun interface LocalRouteResolver {
    suspend fun resolve(routingId: String): String?
}

fun interface RemoteFederationClient {
    suspend fun deliver(
        descriptor: SparrowNodeDescriptor,
        envelope: FederatedEnvelope
    ): FederationAcknowledgement
}

fun interface RemoteIndicatorFederationClient {
    suspend fun deliver(
        descriptor: SparrowNodeDescriptor,
        event: FederatedIndicatorEvent
    ): Boolean
}

fun interface RemoteRouteResolver {
    suspend fun resolve(
        descriptor: SparrowNodeDescriptor,
        routingId: String
    ): String?
}

fun interface MailboxClient {
    suspend fun store(envelope: FederatedEnvelope): FederationAcknowledgement
}
