package com.cbgm.sparrow.feature.chats.presentation.overview.model

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget

data class ConversationListItem(
    val conversationId: String,
    val contactId: String,
    val contactName: String,
    val avatarTarget: AvatarTarget,
    val lastMessage: String = "",
    val hasMessages: Boolean = false,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val lastMessagePreview: LastMessagePreviewUi? = null
)

enum class LastMessagePreviewUi {
    MEDIA,
    LOCATION,
    CONTACT_CARD,
    VOICE,
    ATTACHMENT
}
