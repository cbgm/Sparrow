package com.cbgm.sparrow.feature.membership.domain.model

/** Immutable Membership-owned security and lifecycle data used by Chats verification. */
data class GroupVerificationMembershipContext(
    val security: GroupVerificationSecurityState?,
    val memberKeys: List<GroupVerificationMemberKey>,
    val memberships: List<GroupVerificationMembership>
) {
    val ownsGroup: Boolean get() = memberships.any { it.isOwner }

    fun requireCurrentParticipant(contactId: String): GroupVerificationMemberKey =
        memberKeys.firstOrNull { it.contactId == contactId }
            ?: error("Group participant is not part of the current epoch")

    fun requireCurrentRemoteAdmin(contactId: String): GroupVerificationMemberKey =
        requireCurrentParticipant(contactId).also { key ->
            check(key.isAdmin) { "Group participant is not an admin" }
        }
}

data class GroupVerificationSecurityState(
    val ownerContactId: String?,
    val isLocalAdmin: Boolean
)

data class GroupVerificationMemberKey(
    val contactId: String,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val isAdmin: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupVerificationMemberKey) return false
        if (contactId != other.contactId) return false
        if (isAdmin != other.isAdmin) return false
        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = contactId.hashCode()
        result = 31 * result + isAdmin.hashCode()
        result = 31 * result + encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        return result
    }
}

data class GroupVerificationMembership(
    val contactId: String,
    val sourceInvitationId: String,
    val updatedAtEpochMilliseconds: Long,
    val isOwner: Boolean,
    val isActive: Boolean,
    val isVisiblePending: Boolean
)
