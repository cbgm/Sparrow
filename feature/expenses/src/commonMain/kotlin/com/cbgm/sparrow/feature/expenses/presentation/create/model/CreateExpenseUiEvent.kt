package com.cbgm.sparrow.feature.expenses.presentation.create.model

import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResultUi

sealed interface CreateExpenseUiEvent {
    data class DescriptionChanged(
        val text: String
    ) : CreateExpenseUiEvent

    data class AmountChanged(
        val text: String
    ) : CreateExpenseUiEvent

    data class CurrencySelected(
        val code: String
    ) : CreateExpenseUiEvent

    data class CategorySelected(
        val category: ExpenseCategoryUi
    ) : CreateExpenseUiEvent

    data class PayerSelected(
        val id: String
    ) : CreateExpenseUiEvent

    data class ParticipantToggled(
        val id: String
    ) : CreateExpenseUiEvent

    data class SplitModeSelected(
        val mode: ExpenseSplitModeUi
    ) : CreateExpenseUiEvent

    data class CustomAmountChanged(
        val id: String,
        val text: String
    ) : CreateExpenseUiEvent

    data class RemoveQueuedClicked(
        val index: Int
    ) : CreateExpenseUiEvent

    data class ReceiptScanned(
        val result: MediaSelectionResultUi
    ) : CreateExpenseUiEvent

    data object RemoveReceiptClicked : CreateExpenseUiEvent

    data object SplitPickerClicked : CreateExpenseUiEvent

    data object SplitDoneClicked : CreateExpenseUiEvent

    data object AddAnotherClicked : CreateExpenseUiEvent

    data object SendClicked : CreateExpenseUiEvent

    data object BackClicked : CreateExpenseUiEvent
}
