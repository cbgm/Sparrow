package com.cbgm.sparrow.feature.membership.domain.model

/** What the device already holds for the group an incoming invitation names. */
data class GroupInvitationTarget(
    /** Owners of invitations into this group that the local user has not completed yet. */
    val pendingInviterContactIds: Set<String>,
    /** The device holds the group's live security state: the local user is a member, an admin or the owner. */
    val hasLiveMembership: Boolean
)
