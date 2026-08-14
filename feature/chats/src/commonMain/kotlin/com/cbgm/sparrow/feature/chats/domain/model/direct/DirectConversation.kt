package com.cbgm.sparrow.feature.chats.domain.model.direct

data class DirectConversation(
    val id: String,
    val contactId: String,
    val messages: List<DirectMessage>,
    val unreadCount: Int
) {
    val lastMessage: DirectMessage?
        get() = messages.maxByOrNull(DirectMessage::timestamp)
}
