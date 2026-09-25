package com.cbgm.sparrow.feature.membership.domain.model

/** Read-only Membership view; Chats checks its own local message-history boundaries. */
data class GroupMessageMembershipAccess(
    val isJoinPending: Boolean,
    val isLeavePending: Boolean,
    val isDeleted: Boolean,
    val hasCurrentMembership: Boolean
)
