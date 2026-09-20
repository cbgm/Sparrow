package com.cbgm.sparrow.feature.membership.domain.model

/** Packet-derived member key, detached from Membership's persistence entities. */
data class GroupWelcomeMemberKey(
    val groupId: String,
    val epoch: Int,
    val contactId: String,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val role: String
)
