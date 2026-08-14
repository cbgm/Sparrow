package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberPayload(
    val displayName: String?,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val encryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signingPublicKey: ByteArray,
    val role: String,
    val phoneNumber: String? = null
) {
    init {
        require(role.isNotBlank()) { "Group member role must not be blank" }

        val hasEncryptionKey = encryptionPublicKey.isNotEmpty()
        val hasSigningKey = signingPublicKey.isNotEmpty()

        require(hasEncryptionKey == hasSigningKey) {
            "Group member must contain both public keys or neither"
        }

        require(hasSigningKey || !phoneNumber.isNullOrBlank()) {
            "Group member requires public keys or a phone number"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMemberPayload) return false

        return displayName == other.displayName &&
            encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
            signingPublicKey.contentEquals(other.signingPublicKey) &&
            role == other.role &&
            phoneNumber == other.phoneNumber
    }

    override fun hashCode(): Int {
        var result = displayName?.hashCode() ?: 0
        result = 31 * result + encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + (phoneNumber?.hashCode() ?: 0)
        return result
    }
}
