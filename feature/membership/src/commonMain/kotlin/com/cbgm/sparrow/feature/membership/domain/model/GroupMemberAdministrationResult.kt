package com.cbgm.sparrow.feature.membership.domain.model

data class GroupMemberPromotionResult(
    val groupId: String,
    val contactId: String,
    val epoch: Int,
    val updatedAtEpochMilliseconds: Long
)

data class GroupMemberRemovalResult(
    val groupId: String,
    val contactId: String,
    val epoch: Int,
    val eventId: String,
    val updatedAtEpochMilliseconds: Long,
    val reason: GroupMemberRemovalReason
)

enum class GroupMemberRemovalReason {
    REMOVED,
    LEFT
}

/**
 * A removal the local admin committed earlier: the member is no longer in the current epoch. A retry after a later
 * step failed continues from here instead of removing again. [isRecorded] is false when no owner-side membership
 * row proves the removal (a member another admin invited); only the conversation projection shows it then.
 */
data class GroupCommittedMemberRemoval(
    val result: GroupMemberRemovalResult,
    val isRecorded: Boolean
)
