package com.cbgm.sparrow.feature.membership.domain.model

data class MembershipResult(
    val membershipId: String,
    val sourceInvitationId: String,
    val groupId: String,
    val peerId: String,
    val perspective: MembershipPerspective,
    val status: MembershipStatus,
    val updatedAtEpochMilliseconds: Long
)
