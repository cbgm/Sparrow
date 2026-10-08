package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.usecase.group.CreateGroupExpenseUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.GetGroupExpenseCreationContextUseCase
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.toCreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.domain.ExpenseMoney
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun onUiEvent(event: CreateExpenseUiEvent) {
        if (_uiState.value.isSending) return
        if (_uiState.value.isLoading) {
            if (event == CreateExpenseUiEvent.BackClicked) navigator.popBackStack()
            return
        }
        when (event) {
            is CreateExpenseUiEvent.DescriptionChanged -> change { copy(description = event.text.take(500)) }
            is CreateExpenseUiEvent.AmountChanged -> change { copy(amount = event.text.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(24)) }
            is CreateExpenseUiEvent.PayerSelected -> change { if (participants.any { it.id == event.id }) copy(payerId = event.id) else this }
            is CreateExpenseUiEvent.ParticipantToggled -> change {
                if (participants.none { it.id == event.id }) {
                    this
                } else {
                    copy(selectedParticipantIds = if (event.id in selectedParticipantIds) selectedParticipantIds - event.id else selectedParticipantIds + event.id)
                }
            }
            CreateExpenseUiEvent.CreateClicked -> create()
            CreateExpenseUiEvent.BackClicked -> navigator.popBackStack()
        }
    }

    private fun change(transform: CreateExpenseUiState.() -> CreateExpenseUiState) {
        _uiState.update { it.transform().copy(errorMessage = null).validated() }
    }

    private fun CreateExpenseUiState.validated(): CreateExpenseUiState {
        val amountMinor = ExpenseMoney.parseEuroCents(amount)
        val valid = currencyCode == "EUR" && description.isNotBlank() && amountMinor != null &&
            selectedParticipantIds.isNotEmpty() && amountMinor >= selectedParticipantIds.size &&
            participants.any { it.id == payerId } && !isSending
        return copy(canCreate = valid)
    }

    private fun create() {
        val state = _uiState.value
        if (!state.canCreate) return
        val amountMinor = ExpenseMoney.parseEuroCents(state.amount) ?: return
        _uiState.update { it.copy(isSending = true, canCreate = false, errorMessage = null) }
        viewModelScope.launch {
            createExpense(groupId, state.boardId, state.description, amountMinor, state.payerId, state.selectedParticipantIds)
                .onSuccess { navigator.popBackStack() }
                .onFailure { error ->
                    _uiState.update { it.copy(isSending = false, errorMessage = error.message).validated() }
                }
        }
    }
}
