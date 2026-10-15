package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

/** A member whose readiness this device confirmed; [eventId] keeps the history event idempotent. */
data class GroupParticipantAddition(
    val groupId: String,
    val contactId: String,
    val memberDisplayName: String,
    val epoch: Int,
    val joinedAtEpochMilliseconds: Long,
    val eventId: String
)
