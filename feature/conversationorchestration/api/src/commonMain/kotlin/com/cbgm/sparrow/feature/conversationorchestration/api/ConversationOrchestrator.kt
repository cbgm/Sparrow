package com.cbgm.sparrow.feature.conversationorchestration.api

import com.cbgm.sparrow.feature.conversationorchestration.api.command.ConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationState
import kotlinx.coroutines.flow.Flow

/**
 * The single entry point for every conversation write workflow.
 *
 * Chats submits a [ConversationCommand] and observes the workflow's [ConversationState]s.
 * The orchestration state machine decides every following step on its own; callers never
 * sequence persistence, membership, invitation or delivery operations themselves.
 */
interface ConversationOrchestrator {
    fun execute(command: ConversationCommand): Flow<ConversationState>
}
