package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseRequest
import com.cbgm.sparrow.feature.expenses.domain.usecase.GetExpenseCreationContextUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.SubmitExpenseRequestUseCase
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.nextDraft
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toAmountInput
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toCreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toEditedExpense
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toExpenseAllocations
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toExpenseCategory
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toReceiptImage
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.withReceiptScan
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.withSplitMode
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResultUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.localFilePaths
import com.cbgm.sparrow.feature.ocr.domain.usecase.ScanReceiptUseCase
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_incomplete_draft
import com.cbgm.sparrow.resources.feature_expenses_invalid_amount
import com.cbgm.sparrow.resources.feature_expenses_load_failed
import com.cbgm.sparrow.resources.feature_expenses_receipt_failed
import com.cbgm.sparrow.resources.feature_expenses_send_failed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class CreateExpenseViewModel(
    savedStateHandle: SavedStateHandle,
    private val getContext: GetExpenseCreationContextUseCase,
    private val submitRequest: SubmitExpenseRequestUseCase,
    private val scanReceipt: ScanReceiptUseCase,
    private val mediaFiles: MediaSelectionFileRepository
) : BaseViewModel() {
    private val groupId = savedStateHandle.requireRouteArgument<String>(AppRoute.CreateExpense::groupId.name)
    private val expenseId = savedStateHandle.get<String>(AppRoute.CreateExpense::expenseId.name)
    private var editedExpense: Expense? = null
    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()
    private val mediaCleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
            is CreateExpenseUiEvent.RemoveQueuedClicked -> removeQueued(event.index)
            is CreateExpenseUiEvent.ReceiptScanned -> updateReceipt(event.result)
            CreateExpenseUiEvent.RemoveReceiptClicked -> replaceReceipt(null)
            CreateExpenseUiEvent.SplitPickerClicked -> _uiState.update { it.copy(isSplitOpen = true) }
            CreateExpenseUiEvent.SplitDoneClicked -> _uiState.update { it.copy(isSplitOpen = false) }
            CreateExpenseUiEvent.AddAnotherClicked -> queueDraft()
            CreateExpenseUiEvent.SendClicked -> sendAll()
            CreateExpenseUiEvent.BackClicked ->
                if (state.isSplitOpen) {
                    _uiState.update { it.copy(isSplitOpen = false) }
                } else {
                    navigator.popBackStack()
                }
        }
    }

    private fun removeQueued(index: Int) {
        val removed = _uiState.value.queued.getOrNull(index) ?: return
        _uiState.update { current ->
            current.copy(queued = current.queued.filterIndexed { queuedIndex, _ -> queuedIndex != index })
        }
        removed.receipt?.let { deletePendingSelections(listOf(it)) }
    }

    private fun updateReceipt(result: MediaSelectionResultUi) {
        when (result) {
            MediaSelectionResultUi.Dismissed -> Unit
            is MediaSelectionResultUi.Error -> viewModelScope.launch {
                SparrowLog.showError(getString(Res.string.feature_expenses_receipt_failed))
            }
            is MediaSelectionResultUi.Selected -> {
                val scanned = result.media.filterIsInstance<VisualMediaSelectionUi>()
                val receipt = scanned.firstOrNull { it.type == MediaTypeUi.IMAGE }
                deletePendingSelections(scanned - setOfNotNull(receipt))
                if (receipt != null) {
                    replaceReceipt(receipt)
                    readReceipt(receipt)
                }
            }
        }
    }

    private fun readReceipt(receipt: VisualMediaSelectionUi) {
        viewModelScope.launch {
            scanReceipt(receipt.localFilePath).onSuccess { scan ->
                updateDraft { if (this.receipt?.id == receipt.id) withReceiptScan(scan) else this }
            }
        }
    }

    private fun replaceReceipt(receipt: VisualMediaSelectionUi?) {
        val previous = _uiState.value.draft.receipt
        updateDraft { copy(receipt = receipt) }
        if (previous != null && previous.id != receipt?.id) {
            deletePendingSelections(listOf(previous))
        }
    }

    private fun deletePendingSelections(media: List<VisualMediaSelectionUi>) {
        val storedReceiptId = editedExpense?.receipt?.id
        val pending = media.filterNot { it.id == storedReceiptId }
        if (pending.isEmpty()) return
        mediaCleanupScope.launch {
            pending.flatMap { it.localFilePaths }
                .distinct()
                .forEach { path -> mediaFiles.delete(path) }
        }
    }

    private fun pendingReceipts(state: CreateExpenseUiState): List<VisualMediaSelectionUi> =
        (state.queued + state.draft).mapNotNull(ExpenseDraftUi::receipt)

    override fun onCleared() {
        deletePendingSelections(pendingReceipts(_uiState.value))
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
                    submitRequest(
                        ExpenseRequest.Create(
                            groupId = groupId,
                            boardId = current.boardId,
                            description = next.description,
                            amountMinor = amount,
                            paidByMemberId = next.payerId,
                            allocations = allocations,
                            category = next.category.toExpenseCategory(),
                            receipt = next.receipt?.toReceiptImage()
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
            submitRequest(ExpenseRequest.Edit(groupId, expense))
                .onSuccess {
                    _uiState.update { it.copy(draft = it.draft.copy(receipt = null)) }
                    navigator.popBackStack()
                }
                .onFailure {
                    _uiState.update { it.copy(isSending = false) }
                    SparrowLog.showError(getString(Res.string.feature_expenses_send_failed))
                }
        }
    }
}
