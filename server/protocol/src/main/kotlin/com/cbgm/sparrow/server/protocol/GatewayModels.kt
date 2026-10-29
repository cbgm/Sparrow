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

/**
 * Binds a push token to a routing ID. The client signs routing ID, token, platform and issue time with the
 * identity key the routing ID is derived from. The proof fields default to empty so an unsigned request
 * from an old app gets a clear rejection instead of a parse error.
 */
@Serializable
data class PushDeviceRegistrationRequest(
    @SerialName("routingId")
    val routingId: String,
    val token: String,
    val platform: String,
    val issuedAtEpochMilliseconds: Long = 0L,
    val clientSigningPublicKey: ByteArray = byteArrayOf(),
    val clientSignature: ByteArray = byteArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PushDeviceRegistrationRequest

        if (routingId != other.routingId) return false
        if (token != other.token) return false
        if (platform != other.platform) return false
        if (issuedAtEpochMilliseconds != other.issuedAtEpochMilliseconds) return false
        if (!clientSigningPublicKey.contentEquals(other.clientSigningPublicKey)) return false
        if (!clientSignature.contentEquals(other.clientSignature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = routingId.hashCode()
        result = 31 * result + token.hashCode()
        result = 31 * result + platform.hashCode()
        result = 31 * result + issuedAtEpochMilliseconds.hashCode()
        result = 31 * result + clientSigningPublicKey.contentHashCode()
        result = 31 * result + clientSignature.contentHashCode()
        return result
    }
}

@Serializable
data class PendingTransportEnvelopesResponse(
    val envelopes: List<TransportEnvelope>
)

@Serializable
sealed interface GatewayClientMessage {
    /**
     * Signs in a WebSocket session. The signature answers this connection's [GatewayServerMessage.Challenge],
     * so a client can only register the routing ID derived from a key it holds.
     */
    @Serializable
    @SerialName("register")
    data class Register(
        @SerialName("routingId")
        val routingId: String,
        val connectionId: String,
        val clientSigningPublicKey: ByteArray,
        val clientSignature: ByteArray
    ) : GatewayClientMessage {
        init {
            require(routingId.isNotBlank())
            require(connectionId.isNotBlank())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Register

            if (routingId != other.routingId) return false
            if (connectionId != other.connectionId) return false
            if (!clientSigningPublicKey.contentEquals(other.clientSigningPublicKey)) return false
            if (!clientSignature.contentEquals(other.clientSignature)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = routingId.hashCode()
            result = 31 * result + connectionId.hashCode()
            result = 31 * result + clientSigningPublicKey.contentHashCode()
            result = 31 * result + clientSignature.contentHashCode()
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
    @SerialName("indicator_state")
    data class IndicatorState(
        val recipientId: String,
        val indicatorType: String
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
        val blobExpiresAtEpochMilliseconds: Long
    ) : GatewayClientMessage {
        init {
            require(requestId.isNotBlank())
            require(BLOB_ID.matches(blobId)) { "Invalid blob ID" }
            require(maximumBytes > 0L)
            require(SHA_256_HEX.matches(readCapabilitySha256)) { "Invalid read capability hash" }
            require(SHA_256_HEX.matches(deleteCapabilitySha256)) { "Invalid delete capability hash" }
            require(blobExpiresAtEpochMilliseconds > 0L)
        }

        private companion object {
            val BLOB_ID = Regex("[A-Za-z0-9_-]{16,128}")
            val SHA_256_HEX = Regex("[0-9a-f]{64}")
        }
    }
}

@Serializable
sealed interface GatewayServerMessage {
    /**
     * First frame of every WebSocket session. The single-use [nonce] and this gateway's [nodeId] go into the
     * signed [GatewayClientMessage.Register], so a proof cannot be replayed on another connection or node.
     */
    @Serializable
    @SerialName("challenge")
    data class Challenge(
        val nonce: ByteArray,
        val nodeId: String
    ) : GatewayServerMessage {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Challenge

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
