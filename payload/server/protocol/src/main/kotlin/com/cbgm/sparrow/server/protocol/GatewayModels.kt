package com.cbgm.sparrow.server.protocol

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
    @SerialName("routingId")
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

    @Serializable
    @SerialName("request_blob_upload_ticket")
    data class RequestBlobUploadTicket(
        val requestId: String,
        val blobId: String,
        val maximumBytes: Long,
        val readCapabilitySha256: String,
        val deleteCapabilitySha256: String,
        val blobRetentionMilliseconds: Long
    ) : GatewayClientMessage {
        init {
            require(requestId.isNotBlank())
            require(BLOB_ID.matches(blobId)) { "Invalid blob ID" }
            require(maximumBytes > 0L)
            require(SHA_256_HEX.matches(readCapabilitySha256)) { "Invalid read capability hash" }
            require(SHA_256_HEX.matches(deleteCapabilitySha256)) { "Invalid delete capability hash" }
            require(blobRetentionMilliseconds > 0L)
        }

        private companion object {
            val BLOB_ID = Regex("[A-Za-z0-9_-]{16,128}")
            val SHA_256_HEX = Regex("[0-9a-f]{64}")
        }
    }
}

@Serializable
sealed interface GatewayServerMessage {
    @Serializable
    @SerialName("registered")
    data class Registered(
        @SerialName("routingId")
        val routingId: String
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

    @Serializable
    @SerialName("envelope_accepted")
    data class EnvelopeAccepted(
        val envelopeId: String,
        val expiresAtEpochMilliseconds: Long
    ) : GatewayServerMessage

    @Serializable
    @SerialName("blob_upload_ticket_issued")
    data class BlobUploadTicketIssued(
        val requestId: String,
        val nodeId: String,
        val uploadToken: String,
        val blobExpiresAtEpochMilliseconds: Long
    ) : GatewayServerMessage

    @Serializable
    @SerialName("blob_upload_ticket_rejected")
    data class BlobUploadTicketRejected(
        val requestId: String,
        val code: String,
        val message: String
    ) : GatewayServerMessage

    @Serializable
    @SerialName("error")
    data class Error(
        val code: String,
        val message: String
    ) : GatewayServerMessage
}
