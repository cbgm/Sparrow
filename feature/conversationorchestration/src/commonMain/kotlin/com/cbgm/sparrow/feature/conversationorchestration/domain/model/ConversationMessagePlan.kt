package com.cbgm.sparrow.feature.conversationorchestration.domain.model

sealed interface ConversationMessagePlan {
    data object Send : ConversationMessagePlan

    data object Queue : ConversationMessagePlan

    data class QueueWithAuthorizationFailure(
        val throwable: Throwable
    ) : ConversationMessagePlan
}
