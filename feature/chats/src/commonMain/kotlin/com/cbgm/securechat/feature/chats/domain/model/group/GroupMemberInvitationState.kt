package com.cbgm.securechat.feature.chats.domain.model.group

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
