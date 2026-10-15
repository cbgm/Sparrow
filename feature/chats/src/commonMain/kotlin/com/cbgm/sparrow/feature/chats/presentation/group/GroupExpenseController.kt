package com.cbgm.sparrow.feature.chats.presentation.group

import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupExpensesUseCase
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationMediaController
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupMessageCommand
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toPaymentContext
import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseRequest
import com.cbgm.sparrow.feature.expenses.domain.usecase.ClearExpenseGroupUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExpenseOverviewUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExpenseRequestsUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.PublishExpenseGroupUseCase
import com.cbgm.sparrow.feature.payment.domain.model.PaymentContext
import com.cbgm.sparrow.feature.payment.domain.usecase.ClearPaymentContextUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.CompletePaymentRequestUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.ObservePaymentRequestsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.PublishPaymentContextUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GroupExpenseController(
    private val orchestrator: ConversationOrchestrator,
    private val observeGroupExpenses: ObserveGroupExpensesUseCase,
    private val publishExpenseGroup: PublishExpenseGroupUseCase,
    private val clearExpenseGroup: ClearExpenseGroupUseCase,
    private val observeExpenseRequests: ObserveExpenseRequestsUseCase,
    private val observeExpenseOverview: ObserveExpenseOverviewUseCase,
    private val publishPaymentContext: PublishPaymentContextUseCase,
    private val clearPaymentContext: ClearPaymentContextUseCase,
    private val observePaymentRequests: ObservePaymentRequestsUseCase,
    private val completePaymentRequest: CompletePaymentRequestUseCase,
    private val media: ConversationMediaController
) {
    private lateinit var scope: CoroutineScope
    private lateinit var errors: ConversationErrors
    private lateinit var groupId: String
    private var storedReceiptIds: Set<String> = emptySet()

    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun bind(scope: CoroutineScope, groupId: String, errors: ConversationErrors) {
        this.scope = scope
        this.groupId = groupId
        this.errors = errors
        scope.launch {
            try {
                observeGroupExpenses(groupId)
                    .onEach { group ->
                        storedReceiptIds =
                            group?.expenses.orEmpty().mapNotNullTo(mutableSetOf()) { it.receipt?.id }
                        // Publish before subscribing to the overview: its first value reads this context.
                        publishExpenseGroup(groupId, group).getOrThrow()
                    }
                    .catch {
                        errors.report(it.message ?: "Group expenses could not be loaded")
                        emit(null)
                    }
                    .map { it?.board?.id }
                    .distinctUntilChanged()
                    .flatMapLatest { boardId ->
                        if (boardId == null) {
                            flowOf(null)
                        } else {
                            observeExpenseOverview(groupId)
                                .map<_, PaymentContext?> { it.toPaymentContext() }
                                .catch {
                                    errors.report(it.message ?: "Group expenses could not be loaded")
                                    emit(null)
                                }
                        }
                    }
                    .collect { publishPaymentContext(groupId, it).getOrThrow() }
            } finally {
                clearPaymentContext(groupId)
                clearExpenseGroup(groupId).onFailure {
                    errors.report(it.message ?: "Group expenses could not be cleared")
                }
            }
        }
        scope.launch {
            observePaymentRequests(groupId).collect { request ->
                try {
                    val result = orchestrator.awaitResult(
                        GroupMessageCommand.SendPaymentTransfer(groupId, request.event)
                    ).map { }
                    completePaymentRequest(request, result)
                } finally {
                    // Release the caller even if the conversation scope is cancelled.
                    completePaymentRequest(
                        request,
                        Result.failure(
                            IllegalStateException("Payment request did not complete")
                        )
                    )
                }
            }
        }
        scope.launch {
            observeExpenseRequests()
                .filter { it.groupId == groupId }
                .collect { request ->
                    orchestrator.awaitResult(request.toGroupMessageCommand())
                        .onSuccess { media.deleteLocalPaths(request.pendingReceiptPaths()) }
                        .onFailure { errors.report(it.message ?: "Expense could not be sent") }
                }
        }
    }

    fun activateBoard(currencyCode: String) = execute("Could not activate group expenses") {
        orchestrator.awaitResult(GroupMessageCommand.ActivateExpenses(groupId, currencyCode))
    }

    fun closeBoard() = execute("Could not close group expenses") {
        orchestrator.awaitResult(GroupMessageCommand.CloseExpenses(groupId))
    }

    private fun ExpenseRequest.pendingReceiptPaths(): List<String> {
        val receipt =
            when (this) {
                is ExpenseRequest.Create -> receipt
                is ExpenseRequest.Edit -> expense.receipt?.takeIf { it.id !in storedReceiptIds }
                is ExpenseRequest.Delete -> null
            }
        return listOfNotNull(receipt?.localFilePath)
    }

    private fun execute(fallback: String, action: suspend () -> Result<*>) {
        scope.launch { action().onFailure { errors.report(it.message ?: fallback) } }
    }
}
