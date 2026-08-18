package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel

data class GroupMessageUiModel(
    val bubble: MessageBubbleModel,
    val type: ChatMessageType,
    val senderContactId: String? = null
) {
    val id: String
        get() = bubble.id
}
