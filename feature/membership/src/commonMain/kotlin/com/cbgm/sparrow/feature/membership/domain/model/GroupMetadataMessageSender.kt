package com.cbgm.sparrow.feature.membership.domain.model

/** Membership-owned lookup of an authenticated group message's signing key. */
data class GroupMetadataMessageSender(
    val isLocal: Boolean,
    val memberContactId: String?
)
