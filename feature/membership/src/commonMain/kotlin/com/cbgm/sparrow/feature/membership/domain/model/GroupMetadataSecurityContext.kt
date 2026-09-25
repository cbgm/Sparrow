package com.cbgm.sparrow.feature.membership.domain.model

/** Recipients and epoch authorized by the local Membership security state. */
data class GroupMetadataSendContext(
    val epoch: Int,
    val recipientContactIds: Set<String>
)
