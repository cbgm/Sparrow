package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.TransportEnvelope

interface FederationClient {
    suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement

    suspend fun markStored(envelopeId: String) = Unit
}

enum class PresenceRegistrationResult {
    ACCEPTED,
    REJECTED
}

data class PresenceRegistration(
    val result: PresenceRegistrationResult
)

interface PresenceClient {
    suspend fun register(registration: ClientRouteRegistration): PresenceRegistration

    /** The device a Control Plane routes the phone [alias] to, or null when none does. */
    suspend fun aliasOwner(alias: String): String?

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
