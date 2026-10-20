package com.cbgm.sparrow.feature.conversationorchestration.engine.machine

import com.cbgm.sparrow.feature.conversationorchestration.api.command.ConversationCommand

/** A state machine started by a public [ConversationCommand] submitted through the orchestrator. */
interface ConversationWorkflow<C : ConversationCommand, S : WorkflowStep, O : Any> : WorkflowStateMachine<C, S, O>
