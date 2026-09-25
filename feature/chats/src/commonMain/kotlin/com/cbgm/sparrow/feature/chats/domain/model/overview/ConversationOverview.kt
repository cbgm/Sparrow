package com.cbgm.sparrow.feature.chats.domain.model.overview

data class ConversationOverview(
    val id: String,
    val contactId: String,
    val displayName: String,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long?,
    val updatedAtEpochMilliseconds: Long,
    val unreadCount: Int,
    val participantCount: Int,
    val type: ConversationOverviewType,
    val lastMessagePreview: ConversationOverviewPreview? = null
)

enum class ConversationOverviewType {
    DIRECT,
    GROUP
}

/** The kind of attachment on the latest message, not on an earlier message. */
enum class ConversationOverviewPreview {
    MEDIA,
    LOCATION,
    CONTACT_CARD,
    VOICE,
    ATTACHMENT
}
