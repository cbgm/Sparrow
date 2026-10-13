package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseOverview
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupExpenseOverviewUseCase
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toExpenseOverviewUiState
import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseOverviewUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseOverviewUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_delete_failed
import com.cbgm.sparrow.resources.feature_expenses_load_failed
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class GroupExpenseOverviewViewModel(
    savedStateHandle: SavedStateHandle,
    observeOverview: ObserveGroupExpenseOverviewUseCase,
    private val orchestrator: ConversationOrchestrator
) : BaseViewModel() {
    private val groupId = savedStateHandle.requireRouteArgument<String>(AppRoute.ExpenseOverview::groupId.name)
    private val logger = SparrowLog.withTag("GroupExpenseOverviewViewModel")

    private val overview: StateFlow<GroupExpenseOverview?> =
        observeOverview(groupId)
            .catch { error ->
                logger.error(error) { "Expense overview could not be loaded" }
                SparrowLog.showError(getString(Res.string.feature_expenses_load_failed))
                navigator.popBackStack()
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    val uiState: StateFlow<ExpenseOverviewUiState> =
        overview
            .map { overview -> overview?.toExpenseOverviewUiState() ?: ExpenseOverviewUiState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExpenseOverviewUiState()
            )

    fun onUiEvent(event: ExpenseOverviewUiEvent) {
        when (event) {
            ExpenseOverviewUiEvent.BackClicked -> navigator.popBackStack()
            is ExpenseOverviewUiEvent.EditExpenseClicked ->
                navigator.navigateTo(AppRoute.CreateExpense(groupId, event.expenseId))
            is ExpenseOverviewUiEvent.DeleteExpenseConfirmed -> deleteExpense(event.expenseId)
        }
    }

    private fun deleteExpense(expenseId: String) {
        val expense = overview.value?.expenses?.firstOrNull { it.expenseId == expenseId } ?: return
        viewModelScope.launch {
            orchestrator.awaitResult(GroupMessageCommand.DeleteExpense(groupId, expense))
                .onFailure { error ->
                    logger.error(error) { "Expense could not be deleted" }
                    SparrowLog.showError(getString(Res.string.feature_expenses_delete_failed))
                }
        }
    }
}
