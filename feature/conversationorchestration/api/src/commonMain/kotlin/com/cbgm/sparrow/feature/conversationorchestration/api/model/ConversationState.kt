package com.cbgm.sparrow.feature.conversationorchestration.api.model

/** Progress of one executed [com.cbgm.sparrow.feature.conversationorchestration.api.command.ConversationCommand]. */
sealed interface ConversationState {
    /** The state machine is executing [step]. */
    data class Running(
        val step: String
    ) : ConversationState

    /** The workflow reached its final step. */
    data class Completed(
        val result: ConversationResult
    ) : ConversationState

    /** The workflow stopped; no further step is executed. */
    data class Failed(
        val failure: ConversationFailure
    ) : ConversationState
}
