package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedTypingEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.TransportEnvelope

interface FederationClient {
    suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement

    suspend fun routeTyping(event: FederatedTypingEvent): Boolean = false

    suspend fun markStored(envelopeId: String) = Unit
}

interface PresenceClient {
    suspend fun register(registration: ClientRouteRegistration): Boolean

    suspend fun remove(
        routingId: String,
        connectionId: String
    )
}

interface LegacyPushClient {
    suspend fun store(envelope: TransportEnvelope): Boolean

    suspend fun pending(recipientId: String): List<TransportEnvelope>

    suspend fun acknowledge(
        recipientId: String,
        envelopeId: String
    )
}
