package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("identity")
data class IdentityPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val displayName: String?,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val encryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signingPublicKey: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) {
            "Packet ID must not be blank"
        }

        require(version > 0) {
            "Protocol version must be positive"
        }

        require(encryptionPublicKey.isNotEmpty()) {
            "Encryption public key must not be empty"
        }

        require(signingPublicKey.isNotEmpty()) {
            "Signing public key must not be empty"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is IdentityPacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            displayName == other.displayName &&
            encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
            signingPublicKey.contentEquals(other.signingPublicKey)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()

        result = 31 * result + version

        result = 31 * result + (displayName?.hashCode() ?: 0)

        result = 31 * result + encryptionPublicKey.contentHashCode()

        result = 31 * result + signingPublicKey.contentHashCode()

        return result
    }
}
