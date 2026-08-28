package com.cbgm.sparrow.feature.chats.domain.model.group

data class GroupInvitationOwnerIdentity(
    val contactId: String,
    val encryptionPublicKey: ByteArray?,
    val signingPublicKey: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupInvitationOwnerIdentity

        if (contactId != other.contactId) return false
        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contactId.hashCode()
        result = 31 * result + (encryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (signingPublicKey?.contentHashCode() ?: 0)
        return result
    }
}
