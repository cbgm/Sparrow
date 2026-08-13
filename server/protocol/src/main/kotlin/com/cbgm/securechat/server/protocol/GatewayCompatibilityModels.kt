package com.cbgm.securechat.server.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransportEnvelope(
    val version: Int = 1,
    val envelopeId: String,
    val senderId: String,
    val recipientId: String,
    val payload: String,
    val createdAtEpochMilliseconds: Long
)

@Serializable
data class PushDeviceRegistrationRequest(
    @SerialName("relayId")
    val routingId: String,
    val token: String,
    val platform: String
)

@Serializable
data class PendingTransportEnvelopesResponse(
    val envelopes: List<TransportEnvelope>
)

@Serializable
sealed interface GatewayClientMessage {
    @Serializable
    @SerialName("register")
    data class Register(
        @SerialName("relayId")
        val routingId: String,
        val connectionId: String? = null,
        val generation: Long? = null,
        val expiresAtEpochMilliseconds: Long? = null,
        val aliases: List<String>? = null,
        val clientSigningPublicKey: ByteArray? = null,
        val clientSignature: ByteArray? = null
    ) : GatewayClientMessage {
        init {
            require(routingId.isNotBlank())
            require(connectionId == null || connectionId.isNotBlank())

            val proofFields =
                listOf(
                    generation,
                    expiresAtEpochMilliseconds,
                    clientSigningPublicKey,
                    clientSignature
                )
            require(proofFields.all { it == null } || proofFields.all { it != null }) {
                "Route proof fields must either all be present or all be absent"
            }
            require(generation == null || connectionId != null) {
                "A signed route requires a connection ID"
            }
        }
    }

    @Serializable
    @SerialName("send_envelope")
    data class SendEnvelope(
        val envelope: TransportEnvelope
    ) : GatewayClientMessage

    @Serializable
    @SerialName("send_federated_envelope")
    data class SendFederatedEnvelope(
        val envelope: FederatedEnvelope
    ) : GatewayClientMessage

    @Serializable
    @SerialName("typing_state")
    data class TypingState(
        val recipientId: String,
        val isTyping: Boolean
    ) : GatewayClientMessage

    @Serializable
    @SerialName("acknowledge_envelope")
    data class AcknowledgeEnvelope(
        val envelopeId: String
    ) : GatewayClientMessage

    @Serializable
    @SerialName("refresh_route")
    data class RefreshRoute(
        val registration: ClientRouteRegistration
    ) : GatewayClientMessage
}

@Serializable
sealed interface GatewayServerMessage {
    @Serializable
    @SerialName("registered")
    data class Registered(
        @SerialName("relayId")
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
