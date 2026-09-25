package com.cbgm.sparrow.feature.membership.data.model

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
