package com.cbgm.securechat.feature.chats.domain.model.group

enum class GroupConversationState {
    READY,
    ORPHANED,
    INVITED,
    JOINING,
    WAITING_FOR_MEMBERS,
    DISTRIBUTING_KEYS,
    LEAVING,
    REMOVED,
    DELETED,
    DECLINED,
    EXPIRED,
    FAILED
}
