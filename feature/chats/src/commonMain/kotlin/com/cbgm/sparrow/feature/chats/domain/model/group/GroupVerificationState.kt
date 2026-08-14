package com.cbgm.sparrow.feature.chats.domain.model.group

enum class GroupVerificationMembershipStatus {
    ACTIVE,
    PENDING
}

data class GroupVerificationPair(
    val groupId: String,
    val invitationId: String,
    val contactId: String?,
    val displayName: String,
    val membershipStatus: GroupVerificationMembershipStatus,
    val adminVerifiedParticipant: Boolean,
    val participantVerifiedAdmin: Boolean,
    val updatedAtEpochMilliseconds: Long
)

data class GroupVerificationContext(
    val hasSecurityState: Boolean,
    val isLocalMemberActive: Boolean,
    val isLocalAdmin: Boolean,
    val ownerContactId: String?,
    val ownInvitationId: String?,
    val isLeavePending: Boolean
)

data class GroupVerificationState(
    val context: GroupVerificationContext,
    val ownerDisplayName: String,
    val pairs: List<GroupVerificationPair>
)
