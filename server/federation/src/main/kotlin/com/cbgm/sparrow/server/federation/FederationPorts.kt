package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedTypingEvent
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

fun interface LocalTypingGatewayClient {
    suspend fun deliver(event: FederatedTypingEvent): Boolean
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

fun interface RemoteTypingFederationClient {
    suspend fun deliver(
        descriptor: SparrowNodeDescriptor,
        event: FederatedTypingEvent
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
