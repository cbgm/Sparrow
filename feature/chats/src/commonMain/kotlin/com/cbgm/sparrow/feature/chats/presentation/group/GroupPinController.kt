package com.cbgm.sparrow.feature.chats.presentation.group

import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GroupPinController(
    private val orchestrator: ConversationOrchestrator
) {
    private lateinit var scope: CoroutineScope
    private lateinit var errors: ConversationErrors
    private lateinit var groupId: String

    internal fun bind(scope: CoroutineScope, groupId: String, errors: ConversationErrors) {
        this.scope = scope
        this.groupId = groupId
        this.errors = errors
    }

    fun pin(messageId: String) = execute("Message could not be pinned") {
        orchestrator.awaitResult(GroupMessageCommand.PinMessage(groupId, messageId))
    }

    fun unpin() = execute("Pinned message could not be removed") {
        orchestrator.awaitResult(GroupMessageCommand.UnpinMessage(groupId))
    }

    fun activateExpenseBoard() = execute("Could not activate group expenses") {
        orchestrator.awaitResult(GroupMessageCommand.ActivateExpenses(groupId, EXPENSE_CURRENCY))
    }

    fun closeExpenseBoard() = execute("Could not close group expenses") {
        orchestrator.awaitResult(GroupMessageCommand.CloseExpenses(groupId))
    }

    private fun execute(fallback: String, action: suspend () -> Result<*>) {
        scope.launch { action().onFailure { errors.report(it.message ?: fallback) } }
    }

    private companion object {
        const val EXPENSE_CURRENCY = "EUR"
    }
}
