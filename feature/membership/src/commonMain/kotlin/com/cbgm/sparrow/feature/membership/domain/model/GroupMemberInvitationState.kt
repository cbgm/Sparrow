package com.cbgm.sparrow.feature.membership.domain.model

data class GroupMemberInvitationState(
    val contactId: String,
    val status: GroupMemberInvitationStatus
)

enum class GroupMemberInvitationStatus {
    INVITED,
    ACCEPTED,
    KEY_SENT,
    ACTIVE,
    DECLINED,
    EXPIRED,
    FAILED
}
