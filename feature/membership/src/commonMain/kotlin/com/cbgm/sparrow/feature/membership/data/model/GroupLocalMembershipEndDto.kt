package com.cbgm.sparrow.feature.membership.data.model

internal data class GroupLocalMembershipEndDto(
    val groupId: String,
    val referenceId: String,
    val epoch: Int,
    val endedAtEpochMilliseconds: Long
)
