package com.cbgm.sparrow.feature.transport.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GatewayServerMessage {
    @Serializable
    @SerialName("registered")
    data class Registered(
        @SerialName("routingId")
        val routingId: String,
        val nodeId: String,
        val routeLifetimeMilliseconds: Long,
        val routeRefreshIntervalMilliseconds: Long,
        val serverTimeEpochMilliseconds: Long
    ) : GatewayServerMessage

    @Serializable
    @SerialName("route_registered")
    data class RouteRegistered(
        val aliases: List<String> = emptyList()
    ) : GatewayServerMessage

    @Serializable
    @SerialName("incoming_envelope")
    data class IncomingEnvelope(
        val envelope: TransportEnvelope
    ) : GatewayServerMessage

    @Serializable
    @SerialName("typing_state")
    data class TypingState(
        val senderId: String,
        val isTyping: Boolean
    ) : GatewayServerMessage

    /**
     * Confirms that the gateway accepted the envelope.
     *
     * This does not yet prove that the recipient device read it.
     */
    @Serializable
    @SerialName("envelope_accepted")
    data class EnvelopeAccepted(
        val envelopeId: String,
        val expiresAtEpochMilliseconds: Long
    ) : GatewayServerMessage

    @Serializable
    @SerialName("error")
    data class Error(
        val code: String,
        val message: String
    ) : GatewayServerMessage
}
