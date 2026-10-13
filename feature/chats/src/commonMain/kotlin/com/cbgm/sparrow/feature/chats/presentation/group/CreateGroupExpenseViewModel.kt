package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.usecase.group.GetGroupExpenseCreationContextUseCase
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.nextDraft
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.toAmountInput
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.toCreateExpenseUiState
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.toEditedExpense
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.toExpenseAllocations
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.toExpenseCategory
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.withSplitMode
import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
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
    private val orchestrator: ConversationOrchestrator
) : BaseViewModel() {
    private val groupId = savedStateHandle.requireRouteArgument<String>(AppRoute.CreateExpense::groupId.name)
    private val expenseId = savedStateHandle.get<String>(AppRoute.CreateExpense::expenseId.name)
    private var editedExpense: Expense? = null
    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getContext(groupId, expenseId)
                .onSuccess { context ->
                    editedExpense = context.expense
                    _uiState.value = context.toCreateExpenseUiState()
                }
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
            is CreateExpenseUiEvent.AmountChanged -> updateDraft { copy(amount = event.text.toAmountInput()) }
            is CreateExpenseUiEvent.PayerSelected -> updateDraft {
                if (state.participants.any { it.id == event.id }) copy(payerId = event.id) else this
            }
            is CreateExpenseUiEvent.ParticipantToggled -> updateDraft {
                if (state.participants.none { it.id == event.id }) {
                    this
                } else if (event.id in participantIds) {
                    copy(participantIds = participantIds - event.id, customAmounts = customAmounts - event.id)
                } else {
                    copy(participantIds = participantIds + event.id)
                }
            }
            is CreateExpenseUiEvent.SplitModeSelected -> updateDraft { withSplitMode(event.mode) }
            is CreateExpenseUiEvent.CustomAmountChanged -> updateDraft {
                if (event.id !in participantIds) {
                    this
                } else {
                    copy(customAmounts = customAmounts + (event.id to event.text.toAmountInput()))
                }
            }
            is CreateExpenseUiEvent.RemoveQueuedClicked -> _uiState.update { current ->
                if (event.index !in current.queued.indices) {
                    current
                } else {
                    current.copy(queued = current.queued.filterIndexed { index, _ -> index != event.index })
                }
            }
            CreateExpenseUiEvent.SplitPickerClicked -> navigator.navigateTo(AppRoute.ExpenseSplit(groupId, expenseId))
            CreateExpenseUiEvent.SplitDoneClicked -> navigator.popBackStack()
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
                draft = state.draft.nextDraft()
            )
        }
    }

    private fun reportDraftError(draft: ExpenseDraftUi) {
        viewModelScope.launch {
            val message = if (draft.amount.isNotBlank() && draft.allocationsMinor() == null) {
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
        editedExpense?.let { original ->
            saveEdit(original, state.draft)
            return
        }
        val pending = if (state.canAdd) state.queued + state.draft else state.queued
        _uiState.update {
            it.copy(
                queued = pending,
                draft = state.draft.nextDraft(),
                isSending = true
            )
        }
        viewModelScope.launch {
            while (_uiState.value.queued.isNotEmpty()) {
                val current = _uiState.value
                val next = current.queued.first()
                val amount = next.totalMinor
                val allocations = next.toExpenseAllocations()
                if (amount == null || allocations == null) {
                    _uiState.update { it.copy(isSending = false) }
                    SparrowLog.showError(getString(Res.string.feature_expenses_invalid_amount))
                    return@launch
                }
                val result =
                    orchestrator.awaitResult(
                        GroupMessageCommand.CreateExpense(
                            groupId = groupId,
                            boardId = current.boardId,
                            description = next.description,
                            amountMinor = amount,
                            paidByMemberId = next.payerId,
                            allocations = allocations,
                            category = next.category.toExpenseCategory()
                        )
                    )
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

    private fun saveEdit(original: Expense, draft: ExpenseDraftUi) {
        val expense = draft.toEditedExpense(original) ?: run {
            reportDraftError(draft)
            return
        }
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            orchestrator.awaitResult(GroupMessageCommand.EditExpense(groupId, expense))
                .onSuccess { navigator.popBackStack() }
                .onFailure {
                    _uiState.update { it.copy(isSending = false) }
                    SparrowLog.showError(getString(Res.string.feature_expenses_send_failed))
                }
        }
    }
}
