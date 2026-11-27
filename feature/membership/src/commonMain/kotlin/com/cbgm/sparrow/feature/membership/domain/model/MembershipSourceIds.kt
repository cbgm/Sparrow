package com.cbgm.sparrow.feature.membership.domain.model

/** Membership mints the IDs of group invitations and memberships; other features only recognise them. */
object MembershipSourceIds {
    const val PREFIX = "group-membership"

    fun isGroupMembership(id: String): Boolean = id.startsWith("$PREFIX-")
}
