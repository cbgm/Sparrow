package com.cbgm.sparrow.feature.expenses.presentation.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransferStatus
import com.cbgm.sparrow.core.base.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseOverview
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseRequest
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExpenseOverviewUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.SubmitExpenseRequestUseCase
import com.cbgm.sparrow.feature.expenses.presentation.overview.mapper.toExpenseOverviewUiState
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

class ExpenseOverviewViewModel(
    savedStateHandle: SavedStateHandle,
    observeOverview: ObserveExpenseOverviewUseCase,
    private val submitRequest: SubmitExpenseRequestUseCase
) : BaseViewModel() {
    private val groupId = savedStateHandle.requireRouteArgument<String>(AppRoute.ExpenseOverview::groupId.name)
    private val logger = SparrowLog.withTag("ExpenseOverviewViewModel")

    private val overview: StateFlow<ExpenseOverview?> =
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
            is ExpenseOverviewUiEvent.PaymentClicked -> openPayment(event)
            ExpenseOverviewUiEvent.BackClicked -> navigator.popBackStack()
            is ExpenseOverviewUiEvent.ExpenseClicked ->
                navigator.navigateTo(AppRoute.ExpenseDetail(groupId, event.expenseId))
            is ExpenseOverviewUiEvent.EditExpenseClicked ->
                navigator.navigateTo(AppRoute.CreateExpense(groupId, event.expenseId))
            is ExpenseOverviewUiEvent.DeleteExpenseConfirmed -> deleteExpense(event.expenseId)
        }
    }

    private fun openPayment(event: ExpenseOverviewUiEvent.PaymentClicked) {
        val value = overview.value ?: return
        if (value.board.currencyCode != "EUR" || value.localMemberId !in setOf(event.fromMemberId, event.toMemberId)) return
        val allowed = if (event.transferId != null) {
            value.transfers.any {
                it.transferId == event.transferId && it.status == PaymentTransferStatus.SENT &&
                    it.fromMemberId == event.fromMemberId && it.toMemberId == event.toMemberId && it.amountMinor == event.amountMinor
            }
        } else {
            value.settlements.any {
                it.fromMemberId == value.localMemberId && it.fromMemberId == event.fromMemberId &&
                    it.toMemberId == event.toMemberId && it.amountMinor == event.amountMinor
            }
        }
        if (allowed) navigator.navigateTo(AppRoute.Payment(groupId, value.board.id, event.fromMemberId, event.toMemberId, event.amountMinor, event.transferId))
    }

    private fun deleteExpense(expenseId: String) {
        val expense = overview.value?.expenses?.firstOrNull { it.expenseId == expenseId } ?: return
        viewModelScope.launch {
            submitRequest(ExpenseRequest.Delete(groupId, expense))
                .onFailure { error ->
                    logger.error(error) { "Expense could not be deleted" }
                    SparrowLog.showError(getString(Res.string.feature_expenses_delete_failed))
                }
        }
    }
}
