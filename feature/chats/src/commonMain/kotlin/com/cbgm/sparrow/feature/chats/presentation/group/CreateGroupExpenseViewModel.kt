package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.usecase.group.CreateGroupExpenseUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.GetGroupExpenseCreationContextUseCase
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.toCreateExpenseUiState
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.toExpenseCategory
import com.cbgm.sparrow.feature.expenses.domain.ExpenseMoney
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_incomplete_draft
import com.cbgm.sparrow.resources.feature_expenses_invalid_amount
import com.cbgm.sparrow.resources.feature_expenses_load_failed
import com.cbgm.sparrow.resources.feature_expenses_send_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class CreateGroupExpenseViewModel(
    savedStateHandle: SavedStateHandle,
    private val getContext: GetGroupExpenseCreationContextUseCase,
    private val createExpense: CreateGroupExpenseUseCase
) : BaseViewModel() {
    private val groupId = savedStateHandle.requireRouteArgument<String>(AppRoute.CreateExpense::groupId.name)
    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getContext(groupId)
                .onSuccess { context -> _uiState.value = context.toCreateExpenseUiState() }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    SparrowLog.showError(getString(Res.string.feature_expenses_load_failed))
                }
        }
    }

    fun onUiEvent(event: CreateExpenseUiEvent) {
        val state = _uiState.value
        if (state.isSending) return
        if (state.isLoading) {
            if (event == CreateExpenseUiEvent.BackClicked) navigator.popBackStack()
            return
        }
        when (event) {
            is CreateExpenseUiEvent.DescriptionChanged -> updateDraft { copy(description = event.text.take(500)) }
            is CreateExpenseUiEvent.CategorySelected -> updateDraft { copy(category = event.category) }
            is CreateExpenseUiEvent.AmountChanged -> updateDraft {
                copy(amount = event.text.filter { it.isDigit() || it == '.' || it == ',' }.take(24))
            }
            is CreateExpenseUiEvent.PayerSelected -> updateDraft {
                if (state.participants.any { it.id == event.id }) copy(payerId = event.id) else this
            }
            is CreateExpenseUiEvent.ParticipantToggled -> updateDraft {
                if (state.participants.none { it.id == event.id }) {
                    this
                } else {
                    copy(participantIds = if (event.id in participantIds) participantIds - event.id else participantIds + event.id)
                }
            }
            is CreateExpenseUiEvent.RemoveQueuedClicked -> _uiState.update { current ->
                if (event.index !in current.queued.indices) {
                    current
                } else {
                    current.copy(queued = current.queued.filterIndexed { index, _ -> index != event.index })
                }
            }
            CreateExpenseUiEvent.SplitPickerClicked -> navigator.navigateTo(AppRoute.ExpenseSplit(groupId))
            CreateExpenseUiEvent.AddAnotherClicked -> queueDraft()
            CreateExpenseUiEvent.SendClicked -> sendAll()
            CreateExpenseUiEvent.BackClicked -> navigator.popBackStack()
        }
    }

    private fun updateDraft(update: ExpenseDraftUi.() -> ExpenseDraftUi) {
        _uiState.update { it.copy(draft = it.draft.update()) }
    }

    private fun queueDraft() {
        val state = _uiState.value
        if (!state.canAdd) {
            reportDraftError(state.draft)
            return
        }
        _uiState.update {
            it.copy(
                queued = it.queued + state.draft,
                draft = ExpenseDraftUi(
                    payerId = state.draft.payerId,
                    participantIds = state.draft.participantIds,
                    category = state.draft.category
                )
            )
        }
    }

    private fun reportDraftError(draft: ExpenseDraftUi) {
        viewModelScope.launch {
            val amount = ExpenseMoney.parseEuroCents(draft.amount)
            val message = if (draft.amount.isNotBlank() &&
                (amount == null || amount < draft.participantIds.size)
            ) {
                getString(Res.string.feature_expenses_invalid_amount)
            } else {
                getString(Res.string.feature_expenses_incomplete_draft)
            }
            SparrowLog.showError(message)
        }
    }

    private fun sendAll() {
        val state = _uiState.value
        if (!state.canSend) {
            reportDraftError(state.draft)
            return
        }
        val pending = if (state.canAdd) state.queued + state.draft else state.queued
        _uiState.update {
            it.copy(
                queued = pending,
                draft = ExpenseDraftUi(
                    payerId = state.draft.payerId,
                    participantIds = state.draft.participantIds,
                    category = state.draft.category
                ),
                isSending = true
            )
        }
        viewModelScope.launch {
            while (_uiState.value.queued.isNotEmpty()) {
                val current = _uiState.value
                val next = current.queued.first()
                val amount = ExpenseMoney.parseEuroCents(next.amount)
                if (amount == null) {
                    _uiState.update { it.copy(isSending = false) }
                    SparrowLog.showError(getString(Res.string.feature_expenses_invalid_amount))
                    return@launch
                }
                val result = createExpense(groupId, current.boardId, next.description, amount, next.payerId, next.participantIds, next.category.toExpenseCategory())
                if (result.isFailure) {
                    _uiState.update { it.copy(isSending = false) }
                    SparrowLog.showError(getString(Res.string.feature_expenses_send_failed))
                    return@launch
                }
                _uiState.update { it.copy(queued = it.queued.drop(1)) }
            }
            navigator.popBackStack()
        }
    }
}
