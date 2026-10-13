package com.cbgm.sparrow.feature.conversationorchestration.engine.machine

import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.ConversationFailureMapper

/** The decision a state machine takes after a step reported its outcome. */
sealed interface Transition<out S : WorkflowStep> {
    data class Next<S : WorkflowStep>(
        val step: S
    ) : Transition<S>

    data class Complete(
        val result: ConversationResult = ConversationResult.Done
    ) : Transition<Nothing>

    data class Reject(
        val failure: ConversationFailure
    ) : Transition<Nothing> {
        companion object {
            /** Rejects with the failure an unexpected [error] maps to, keeping the error as cause. */
            fun because(error: Throwable): Reject = Reject(ConversationFailureMapper.toFailure(error))
        }
    }
}
