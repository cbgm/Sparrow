package com.cbgm.sparrow.feature.membership.domain.model

/** Membership-only result handed to conversation orchestration for local chat cleanup. */
data class GroupLocalMembershipEnd(
    val groupId: String,
    val referenceId: String,
    val epoch: Int,
    val endedAtEpochMilliseconds: Long
)
