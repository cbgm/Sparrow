package com.cbgm.sparrow.feature.membership.domain.model

/** Membership-owned state needed to describe the local member's verification participation. */
data class MembershipVerificationSnapshot(
    val hasSecurityState: Boolean,
    val isSecurityAdmin: Boolean,
    val isSecurityMemberActive: Boolean,
    val hasOwnerMembership: Boolean,
    val securityOwnerContactId: String?,
    val memberContactId: String?,
    val memberInvitationId: String?,
    val isMemberLeavePending: Boolean
)
