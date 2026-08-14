package com.cbgm.securechat.feature.chats.data.group.invitation

enum class GroupInvitationStatus {
    INVITE_SENT,
    INVITE_RECEIVED,
    WAITING_FOR_IDENTITY,
    IDENTITY_READY,
    AWAITING_ACCEPTANCE,
    JOIN_SENT,
    WELCOME_SENT,
    WAITING_FOR_ACTIVATION,
    LEAVE_SENT,
    DECLINED,
    EXPIRED,
    FAILED,
    REMOVED,
    GROUP_DELETED,
    ACTIVE
}

enum class GroupInvitationDirection {
    OUTGOING,
    INCOMING
}
