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
)

data class GroupVerificationMembership(
    val contactId: String,
    val sourceInvitationId: String,
    val updatedAtEpochMilliseconds: Long,
    val isOwner: Boolean,
    val isActive: Boolean,
    val isVisiblePending: Boolean
)
