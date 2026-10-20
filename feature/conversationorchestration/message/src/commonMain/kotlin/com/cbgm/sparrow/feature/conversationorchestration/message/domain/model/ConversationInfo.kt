package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

data class ConversationInfo(
    val id: String,
    val isGroup: Boolean,
    val title: String?
)
