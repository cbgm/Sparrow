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
