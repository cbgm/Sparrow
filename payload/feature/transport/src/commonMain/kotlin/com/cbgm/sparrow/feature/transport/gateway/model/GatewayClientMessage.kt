package com.cbgm.sparrow.feature.transport.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@kotlinx.serialization.InternalSerializationApi
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Register

            if (generation != other.generation) return false
            if (expiresAtEpochMilliseconds != other.expiresAtEpochMilliseconds) return false
            if (routingId != other.routingId) return false
            if (connectionId != other.connectionId) return false
            if (aliases != other.aliases) return false
            if (!clientSigningPublicKey.contentEquals(other.clientSigningPublicKey)) return false
            if (!clientSignature.contentEquals(other.clientSignature)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = generation?.hashCode() ?: 0
            result = 31 * result + (expiresAtEpochMilliseconds?.hashCode() ?: 0)
            result = 31 * result + routingId.hashCode()
            result = 31 * result + (connectionId?.hashCode() ?: 0)
            result = 31 * result + (aliases?.hashCode() ?: 0)
            result = 31 * result + (clientSigningPublicKey?.contentHashCode() ?: 0)
            result = 31 * result + (clientSignature?.contentHashCode() ?: 0)
            return result
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
    @SerialName("request_blob_upload_ticket")
    data class RequestBlobUploadTicket(
        val requestId: String,
        val blobId: String,
        val maximumBytes: Long,
        val readCapabilitySha256: String,
        val deleteCapabilitySha256: String,
        val blobRetentionMilliseconds: Long
    ) : GatewayClientMessage

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
