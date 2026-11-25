package com.cbgm.sparrow.feature.transport.domain.model

/** What a letter from this device's invitation mailbox carries, after its sender was proven. */
data class InvitationLetterContent(
    val senderRoutingId: String,
    val payload: String
)
