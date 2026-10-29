package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedIndicatorEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.TransportEnvelope

interface FederationClient {
    suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement

    suspend fun routeIndicator(event: FederatedIndicatorEvent): Boolean = false

    suspend fun markStored(envelopeId: String) = Unit
}

enum class PresenceRegistrationResult {
    ACCEPTED,
    REJECTED,
    PHONE_VERIFICATION_FAILED
}

/** [aliases] are the route's phone aliases the presence directory accepted. */
data class PresenceRegistration(
    val result: PresenceRegistrationResult,
    val aliases: List<String> = emptyList()
)

interface PresenceClient {
    suspend fun register(registration: ClientRouteRegistration): PresenceRegistration

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
