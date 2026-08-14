package com.cbgm.securechat.feature.chats.domain.model.overview

data class ConversationOverview(
    val id: String,
    val contactId: String,
    val displayName: String,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long?,
    val updatedAtEpochMilliseconds: Long,
    val unreadCount: Int,
    val participantCount: Int,
    val type: ConversationOverviewType
)

enum class ConversationOverviewType {
    DIRECT,
    GROUP
}
