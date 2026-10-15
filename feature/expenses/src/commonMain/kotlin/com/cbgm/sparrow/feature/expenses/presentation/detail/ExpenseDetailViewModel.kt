package com.cbgm.sparrow.feature.expenses.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseRequest
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExpenseOverviewUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.SubmitExpenseRequestUseCase
import com.cbgm.sparrow.feature.expenses.presentation.detail.mapper.toExpenseDetailUiState
import com.cbgm.sparrow.feature.expenses.presentation.detail.model.ExpenseDetailUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.detail.model.ExpenseDetailUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_delete_failed
import com.cbgm.sparrow.resources.feature_expenses_load_failed
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class ExpenseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    observeOverview: ObserveExpenseOverviewUseCase,
    private val submitRequest: SubmitExpenseRequestUseCase
) : BaseViewModel() {
    private val groupId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.ExpenseDetail::groupId.name)
    private val expenseId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.ExpenseDetail::expenseId.name)
    private val logger = SparrowLog.withTag("ExpenseDetailViewModel")
    private var shownExpense: Expense? = null

    val uiState: StateFlow<ExpenseDetailUiState> =
        observeOverview(groupId)
            .map { overview ->
                overview to overview.expenses.firstOrNull { it.expenseId == expenseId }
            }
            .onEach { (_, expense) ->
                if (expense == null) navigator.popBackStack()
                shownExpense = expense
            }.map { (overview, expense) ->
                expense?.let { overview.toExpenseDetailUiState(it) } ?: ExpenseDetailUiState()
            }
            .catch { error ->
                logger.error(error) { "Expense could not be loaded" }
                SparrowLog.showError(getString(Res.string.feature_expenses_load_failed))
                navigator.popBackStack()
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExpenseDetailUiState()
            )

    fun onUiEvent(event: ExpenseDetailUiEvent) {
        when (event) {
            ExpenseDetailUiEvent.BackClicked -> navigator.popBackStack()
            ExpenseDetailUiEvent.EditClicked ->
                navigator.navigateTo(AppRoute.CreateExpense(groupId, expenseId))
            ExpenseDetailUiEvent.DeleteConfirmed -> deleteExpense()
        }
    }

    private fun deleteExpense() {
        val expense = shownExpense ?: return
        viewModelScope.launch {
            submitRequest(ExpenseRequest.Delete(groupId, expense))
                .onFailure { error ->
                    logger.error(error) { "Expense could not be deleted" }
                    SparrowLog.showError(getString(Res.string.feature_expenses_delete_failed))
                }
        }
    }
}
