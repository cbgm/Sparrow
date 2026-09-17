package com.cbgm.sparrow.feature.membership.domain.model

enum class GroupConversationState {
    READY,
    JOINING,
    WAITING_FOR_MEMBERS,
    DISTRIBUTING_KEYS,
    LEAVING,
    REMOVED,
    DELETED,
    FAILED
}
