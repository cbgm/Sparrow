package com.cbgm.sparrow.server.protocol

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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeliveryRoute

        if (sequence != other.sequence) return false
        if (expiresAtEpochMilliseconds != other.expiresAtEpochMilliseconds) return false
        if (routeId != other.routeId) return false
        if (nodeId != other.nodeId) return false
        if (nodeEndpoint != other.nodeEndpoint) return false
        if (mailboxId != other.mailboxId) return false
        if (sendCapability != other.sendCapability) return false
        if (!identitySignature.contentEquals(other.identitySignature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sequence.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + routeId.hashCode()
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + nodeEndpoint.hashCode()
        result = 31 * result + mailboxId.hashCode()
        result = 31 * result + sendCapability.hashCode()
        result = 31 * result + identitySignature.contentHashCode()
        return result
    }
}

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
data class FederatedIndicatorEvent(
    val senderRoutingId: String,
    val recipientRoutingId: String,
    val indicatorType: String
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
