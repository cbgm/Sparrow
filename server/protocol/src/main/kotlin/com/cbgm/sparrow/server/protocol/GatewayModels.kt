package com.cbgm.sparrow.server.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Checked like the app checks it, so an envelope no app can read is refused instead of stored and replayed. */
@Serializable
data class TransportEnvelope(
    val version: Int = 1,
    val envelopeId: String,
    val senderId: String,
    val recipientId: String,
    val payload: String,
    val createdAtEpochMilliseconds: Long
) {
    init {
        require(version > 0)
        require(envelopeId.isNotBlank())
        require(senderId.isNotBlank())
        require(recipientId.isNotBlank())
        require(payload.isNotBlank())
        require(createdAtEpochMilliseconds >= 0L)
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
    data object RouteRegistered : GatewayServerMessage

    @Serializable
    @SerialName("incoming_envelope")
    data class IncomingEnvelope(
        val envelope: TransportEnvelope
    ) : GatewayServerMessage

    @Serializable
    @SerialName("envelope_accepted")
    data class EnvelopeAccepted(
        val envelopeId: String,
        val expiresAtEpochMilliseconds: Long
    ) : GatewayServerMessage

    @Serializable
    @SerialName("error")
    data class Error(
        val code: String,
        val message: String,
        /** Set when the error refuses one sent envelope, so the client fails that send at once. */
        val envelopeId: String? = null
    ) : GatewayServerMessage
}
