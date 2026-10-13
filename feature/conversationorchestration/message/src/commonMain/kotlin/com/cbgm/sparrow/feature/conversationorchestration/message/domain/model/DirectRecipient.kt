package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

/** The authorized contact a direct message is prepared for. */
data class DirectRecipient(
    val target: DirectConversationTarget,
    val displayName: String
)
