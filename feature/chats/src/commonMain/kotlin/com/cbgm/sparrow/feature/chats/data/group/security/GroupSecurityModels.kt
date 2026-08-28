package com.cbgm.sparrow.feature.chats.data.group.security

import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket

data class GroupWelcomeRecipientDto(
    val contactId: String,
    val invitationId: String,
    val encryptionPublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupWelcomeRecipientDto

        if (contactId != other.contactId) return false
        if (invitationId != other.invitationId) return false
        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contactId.hashCode()
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + encryptionPublicKey.contentHashCode()
        return result
    }
}

data class CreatedGroupSecurityDto(
    val welcomePacketsByContactId: Map<String, GroupCreatedPacket>
)

data class OpenedGroupWelcomeDto(
    val packet: GroupCreatedPacket,
    val groupKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenedGroupWelcomeDto

        if (packet != other.packet) return false
        if (!groupKey.contentEquals(other.groupKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packet.hashCode()
        result = 31 * result + groupKey.contentHashCode()
        return result
    }
}

data class SecuredGroupMessageDto(
    val epoch: Int,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
    val senderSignature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecuredGroupMessageDto

        if (epoch != other.epoch) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!senderSignature.contentEquals(other.senderSignature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = epoch
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + senderSignature.contentHashCode()
        return result
    }
}
