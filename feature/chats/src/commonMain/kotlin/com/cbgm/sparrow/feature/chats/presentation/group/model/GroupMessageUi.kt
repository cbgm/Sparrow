package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType

data class GroupMessageUi(
    val type: ChatMessageType,
    val senderContactId: String? = null
)
