package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

/**
 * The recipients a queued group message still has to reach. On the [isFirstDelivery] no recipient state
 * exists yet, so the caller persists the states for the new packets before enqueueing them.
 */
data class UndeliveredGroupRecipients(
    val contactIds: List<String>,
    val isFirstDelivery: Boolean
)
