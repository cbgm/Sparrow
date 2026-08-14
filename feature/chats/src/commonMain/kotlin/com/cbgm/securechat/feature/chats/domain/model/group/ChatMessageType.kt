package com.cbgm.securechat.feature.chats.domain.model.group

enum class ChatMessageType {
    USER,
    GROUP_MEMBER_ADDED,
    GROUP_MEMBER_REMOVED,
    LOCAL_GROUP_MEMBERSHIP_REMOVED,
    GROUP_MEMBER_LEFT,
    LOCAL_GROUP_MEMBERSHIP_LEFT
}
