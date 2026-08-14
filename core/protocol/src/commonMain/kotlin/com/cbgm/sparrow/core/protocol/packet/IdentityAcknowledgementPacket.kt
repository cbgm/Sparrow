package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("identity_acknowledgement")
data class IdentityAcknowledgementPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val senderSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val acknowledgedEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val acknowledgedSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) {
            "Packet ID must not be blank"
        }

        require(version > 0) {
            "Protocol version must be positive"
        }

        require(senderSigningPublicKey.isNotEmpty()) {
            "Sender signing public key must not be empty"
        }

        require(acknowledgedEncryptionPublicKey.isNotEmpty()) {
            "Acknowledged encryption key must not be empty"
        }

        require(acknowledgedSigningPublicKey.isNotEmpty()) {
            "Acknowledged signing key must not be empty"
        }

        require(signature.isNotEmpty()) {
            "Signature must not be empty"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is IdentityAcknowledgementPacket) return false

        return packetId ==
            other.packetId &&
            version == other.version &&
            senderSigningPublicKey.contentEquals(other.senderSigningPublicKey) &&
            acknowledgedEncryptionPublicKey.contentEquals(other.acknowledgedEncryptionPublicKey) &&
            acknowledgedSigningPublicKey.contentEquals(other.acknowledgedSigningPublicKey) &&
            signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()

        result = 31 * result + version

        result = 31 * result + senderSigningPublicKey.contentHashCode()

        result = 31 * result + acknowledgedEncryptionPublicKey.contentHashCode()

        result = 31 * result + acknowledgedSigningPublicKey.contentHashCode()

        result = 31 * result + signature.contentHashCode()

        return result
    }
}
