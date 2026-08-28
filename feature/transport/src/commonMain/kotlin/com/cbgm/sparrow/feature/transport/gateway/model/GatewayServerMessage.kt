package com.cbgm.sparrow.feature.transport.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@kotlinx.serialization.InternalSerializationApi
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
