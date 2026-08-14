package com.cbgm.securechat.server.protocol

import kotlinx.serialization.Serializable

@Serializable
data class DeliveryRoute(
    val routeId: String,
    val nodeId: String,
    val nodeEndpoint: String,
    val mailboxId: String,
    val sendCapability: String,
    val sequence: Long,
    val expiresAtEpochMilliseconds: Long,
    val identitySignature: ByteArray
)

@Serializable
data class FederatedEnvelope(
    val envelopeId: String,
    val senderRoutingId: String,
    val recipientDeviceRoutingId: String,
    val mailboxRoute: DeliveryRoute? = null,
    val encryptedPayload: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long
) {
    init {
        require(envelopeId.isNotBlank())
        require(senderRoutingId.isNotBlank())
        require(recipientDeviceRoutingId.isNotBlank())
        require(encryptedPayload.isNotBlank())
        require(expiresAtEpochMilliseconds > createdAtEpochMilliseconds)
    }
}

@Serializable
data class FederatedTypingEvent(
    val senderRoutingId: String,
    val recipientRoutingId: String,
    val isTyping: Boolean
) {
    init {
        require(senderRoutingId.isNotBlank())
        require(recipientRoutingId.isNotBlank())
    }
}

@Serializable
enum class EnvelopeAcceptanceState {
    QUEUED_AT_GATEWAY,
    STORED_AT_DESTINATION,
    PROCESSED_BY_RECIPIENT
}

@Serializable
data class FederationAcknowledgement(
    val envelopeId: String,
    val state: EnvelopeAcceptanceState,
    val duplicate: Boolean = false
)

@Serializable
data class CreateMailboxRequest(
    val nodeId: String,
    val nodeEndpoint: String,
    val routeSequence: Long = 0L,
    val expiresAtEpochMilliseconds: Long
)

@Serializable
data class CreateMailboxResponse(
    val deliveryRoute: DeliveryRoute,
    val retrievalCapability: String
)

@Serializable
data class MailboxEnvelopeRequest(
    val envelope: FederatedEnvelope
)

@Serializable
data class MailboxEnvelopesResponse(
    val envelopes: List<FederatedEnvelope>
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String
)
