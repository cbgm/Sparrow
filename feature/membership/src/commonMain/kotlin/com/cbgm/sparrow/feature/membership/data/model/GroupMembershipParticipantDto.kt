package com.cbgm.sparrow.feature.membership.data.model

internal data class GroupMembershipParticipantDto(
    val contactId: String,
    val role: String,
    val joinedAtEpochMilliseconds: Long
)
