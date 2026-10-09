package com.cbgm.sparrow.feature.expenses.presentation.create.model

sealed interface CreateExpenseUiEvent {
    data class DescriptionChanged(
        val text: String
    ) : CreateExpenseUiEvent

    data class AmountChanged(
        val text: String
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

    data class RemoveQueuedClicked(
        val index: Int
    ) : CreateExpenseUiEvent

    data object SplitPickerClicked : CreateExpenseUiEvent

    data object AddAnotherClicked : CreateExpenseUiEvent

    data object SendClicked : CreateExpenseUiEvent

    data object BackClicked : CreateExpenseUiEvent
}
