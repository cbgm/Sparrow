package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.ClientRoutingResult
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederatedTypingEvent
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor

fun interface PresenceDirectoryClient {
    suspend fun resolve(routingId: String): ClientRoutingResult
}

fun interface NodeRegistryClient {
    suspend fun find(nodeId: String): SecureChatNodeDescriptor?
}

fun interface PeerNodeDirectory {
    suspend fun peers(): List<SecureChatNodeDescriptor>
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
        descriptor: SecureChatNodeDescriptor,
        envelope: FederatedEnvelope
    ): FederationAcknowledgement
}

fun interface RemoteTypingFederationClient {
    suspend fun deliver(
        descriptor: SecureChatNodeDescriptor,
        event: FederatedTypingEvent
    ): Boolean
}

fun interface RemoteRouteResolver {
    suspend fun resolve(
        descriptor: SecureChatNodeDescriptor,
        routingId: String
    ): String?
}

fun interface MailboxClient {
    suspend fun store(envelope: FederatedEnvelope): FederationAcknowledgement
}
