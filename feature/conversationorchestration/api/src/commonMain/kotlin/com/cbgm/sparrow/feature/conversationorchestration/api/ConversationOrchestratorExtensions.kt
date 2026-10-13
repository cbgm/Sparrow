package com.cbgm.sparrow.feature.conversationorchestration.api

import com.cbgm.sparrow.feature.conversationorchestration.api.command.ConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureException
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationState
import kotlinx.coroutines.flow.first

/** Runs [command] to its end and returns the final result, or the failure as [ConversationFailureException]. */
suspend fun ConversationOrchestrator.awaitResult(command: ConversationCommand): Result<ConversationResult> =
    when (val finalState = execute(command).first { state -> state !is ConversationState.Running }) {
        is ConversationState.Completed -> Result.success(finalState.result)
        is ConversationState.Failed -> Result.failure(ConversationFailureException(finalState.failure))
        is ConversationState.Running -> error("A running state is never final")
    }
