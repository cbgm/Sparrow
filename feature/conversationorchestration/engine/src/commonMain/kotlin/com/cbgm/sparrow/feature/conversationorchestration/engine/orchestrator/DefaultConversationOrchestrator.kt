package com.cbgm.sparrow.feature.conversationorchestration.engine.orchestrator

import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.command.ConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationState
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import kotlinx.coroutines.flow.Flow

/** Hands every public command to the engine, which runs the one workflow registered for its type. */
internal class DefaultConversationOrchestrator(
    private val engine: WorkflowEngine
) : ConversationOrchestrator {
    override fun execute(command: ConversationCommand): Flow<ConversationState> = engine.execute(command)
}
