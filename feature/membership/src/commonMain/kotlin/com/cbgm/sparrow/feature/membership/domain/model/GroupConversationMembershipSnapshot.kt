package com.cbgm.sparrow.feature.membership.domain.model

/** Read-only Membership state for conversation rendering, never database entities. */
data class GroupConversationMembershipSnapshot(
    val participantContactIds: List<String>,
    val memberships: List<GroupMemberLifecycleSnapshot>
)

data class GroupMemberLifecycleSnapshot(
    val contactId: String,
    val sourceInvitationId: String,
    val status: String,
    val createdAtEpochMilliseconds: Long
)
