package com.cbgm.sparrow.feature.transport.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@kotlinx.serialization.InternalSerializationApi
@Serializable
sealed interface GatewayServerMessage {
    /**
     * First frame of every session. The single-use [nonce] and the gateway's [nodeId] are signed into
     * [GatewayClientMessage.Register], so a proof cannot be replayed on another connection or node.
     */
    @Serializable
    @SerialName("challenge")
    data class Challenge(
        val nonce: ByteArray,
        val nodeId: String
    ) : GatewayServerMessage {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Challenge) return false

            return nodeId == other.nodeId && nonce.contentEquals(other.nonce)
        }

        override fun hashCode(): Int = 31 * nonce.contentHashCode() + nodeId.hashCode()
    }

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
    @SerialName("indicator_state")
    data class IndicatorState(
        val senderId: String,
        val indicatorType: String
    ) : GatewayServerMessage

    /**
     * Confirms that the gateway accepted the envelope and owns delivery retries until the
     * server-declared expiry deadline.
     */
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
