package com.cbgm.sparrow.feature.membership.data

import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPeerDto

fun GroupMembershipPeerDto.hasMutualGroupIdentity(): Boolean = hasMutualIdentity

fun GroupMembershipPeerDto.groupMembershipDisplayName(): String =
    displayName?.trim()?.takeIf(String::isNotEmpty)
        ?: preferredPhoneNumber?.trim()?.takeIf(String::isNotEmpty)
        ?: "Member"

fun GroupMembershipPeerDto.requireGroupPhoneNumber(): String =
    preferredPhoneNumber?.trim()?.takeIf(String::isNotEmpty)
        ?: phoneNumbers
            .firstOrNull()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        ?: error("Membership peer has no phone number: $id")
