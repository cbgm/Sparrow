package com.cbgm.sparrow.feature.transport.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GatewayServerMessage {
    @Serializable
    @SerialName("registered")
    data class Registered(
        @SerialName("routingId")
        val routingId: String
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
        val envelopeId: String
    ) : GatewayServerMessage

    @Serializable
    @SerialName("error")
    data class Error(
        val code: String,
        val message: String
    ) : GatewayServerMessage
}
