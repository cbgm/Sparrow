package com.cbgm.securechat.feature.transport.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GatewayClientMessage {
    /**
     * Sent immediately after opening the WebSocket.
     */
    @Serializable
    @SerialName("register")
    data class Register(
        @SerialName("routingId")
        val routingId: String,
        val connectionId: String? = null,
        val generation: Long? = null,
        val expiresAtEpochMilliseconds: Long? = null,
        val aliases: List<String>? = null,
        val clientSigningPublicKey: ByteArray? = null,
        val clientSignature: ByteArray? = null
    ) : GatewayClientMessage {
        init {
            require(routingId.isNotBlank()) {
                "Routing ID must not be blank"
            }
            require(connectionId == null || connectionId.isNotBlank()) {
                "Connection ID must not be blank"
            }

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
    @SerialName("refresh_route")
    data class RefreshRoute(
        val registration: ClientRouteRegistration
    ) : GatewayClientMessage

    /**
     * Requests delivery of one opaque envelope.
     */
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
    ) : GatewayClientMessage {
        init {
            require(recipientId.isNotBlank()) {
                "Recipient routing ID must not be blank"
            }
        }
    }

    @Serializable
    @SerialName("acknowledge_envelope")
    data class AcknowledgeEnvelope(
        val envelopeId: String
    ) : GatewayClientMessage {
        init {
            require(envelopeId.isNotBlank()) {
                "Envelope ID must not be blank"
            }
        }
    }
}
