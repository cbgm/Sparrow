package com.cbgm.sparrow.feature.chats.presentation.group

import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupExpensesUseCase
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.expenses.domain.usecase.ClearExpenseGroupUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExpenseRequestsUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.PublishExpenseGroupUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class GroupExpenseController(
    private val orchestrator: ConversationOrchestrator,
    private val observeGroupExpenses: ObserveGroupExpensesUseCase,
    private val publishExpenseGroup: PublishExpenseGroupUseCase,
    private val clearExpenseGroup: ClearExpenseGroupUseCase,
    private val observeExpenseRequests: ObserveExpenseRequestsUseCase
) {
    private lateinit var scope: CoroutineScope
    private lateinit var errors: ConversationErrors
    private lateinit var groupId: String

    internal fun bind(scope: CoroutineScope, groupId: String, errors: ConversationErrors) {
        this.scope = scope
        this.groupId = groupId
        this.errors = errors
        scope.launch {
            try {
                observeGroupExpenses(groupId)
                    .catch { errors.report(it.message ?: "Group expenses could not be loaded") }
                    .collect { publishExpenseGroup(groupId, it) }
            } finally {
                clearExpenseGroup(groupId)
            }
        }
        scope.launch {
            observeExpenseRequests()
                .filter { it.groupId == groupId }
                .collect { request ->
                    orchestrator.awaitResult(request.toGroupMessageCommand())
                        .onFailure { errors.report(it.message ?: "Expense could not be sent") }
                }
        }
    }

    fun activateBoard() = execute("Could not activate group expenses") {
        orchestrator.awaitResult(GroupMessageCommand.ActivateExpenses(groupId, EXPENSE_CURRENCY))
    }

    fun closeBoard() = execute("Could not close group expenses") {
        orchestrator.awaitResult(GroupMessageCommand.CloseExpenses(groupId))
    }

    private fun execute(fallback: String, action: suspend () -> Result<*>) {
        scope.launch { action().onFailure { errors.report(it.message ?: fallback) } }
    }

    private companion object {
        const val EXPENSE_CURRENCY = "EUR"
    }
}
