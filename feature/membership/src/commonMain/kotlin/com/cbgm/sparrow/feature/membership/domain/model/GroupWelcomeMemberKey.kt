package com.cbgm.sparrow.feature.membership.domain.model

/** Packet-derived member key, detached from Membership's persistence entities. */
data class GroupWelcomeMemberKey(
    val groupId: String,
    val epoch: Int,
    val contactId: String,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val role: String,
    val phoneNumber: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupWelcomeMemberKey) return false
        if (groupId != other.groupId) return false
        if (epoch != other.epoch) return false
        if (contactId != other.contactId) return false
        if (role != other.role) return false
        if (phoneNumber != other.phoneNumber) return false
        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + epoch.hashCode()
        result = 31 * result + contactId.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + (phoneNumber?.hashCode() ?: 0)
        result = 31 * result + encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        return result
    }
}
