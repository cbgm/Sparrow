package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

/** An owner-signed activation of another member, identified by [activationId]. */
data class GroupParticipantActivation(
    val groupId: String,
    val contactId: String,
    val role: String,
    val epoch: Int,
    val activationId: String,
    val memberDisplayName: String,
    val joinedAtEpochMilliseconds: Long
)
