package com.cbgm.sparrow.feature.transport.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@kotlinx.serialization.InternalSerializationApi
@Serializable
sealed interface GatewayClientMessage {
    /**
     * Signs in after the gateway's [GatewayServerMessage.Challenge]. [clientSignature] covers
     * [gatewaySessionPayload], so the gateway only accepts the routing ID of the key holder.
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
            require(routingId.isNotBlank()) {
                "Routing ID must not be blank"
            }
            require(connectionId.isNotBlank()) {
                "Connection ID must not be blank"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Register) return false

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
    @SerialName("indicator_state")
    data class IndicatorState(
        val recipientId: String,
        val indicatorType: String
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
        val blobExpiresAtEpochMilliseconds: Long
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
