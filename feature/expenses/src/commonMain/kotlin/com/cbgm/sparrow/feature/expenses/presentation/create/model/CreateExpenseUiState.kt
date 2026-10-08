package com.cbgm.sparrow.feature.expenses.presentation.create.model

data class ExpenseParticipantUi(
    val id: String,
    val displayName: String,
    val isLocal: Boolean = false
)

data class CreateExpenseUiState(
    val boardId: String = "",
    val currencyCode: String = "EUR",
    val description: String = "",
    val amount: String = "",
    val payerId: String = "",
    val participants: List<ExpenseParticipantUi> = emptyList(),
    val selectedParticipantIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val canCreate: Boolean = false,
    val errorMessage: String? = null
)

sealed interface CreateExpenseUiEvent {
    data class DescriptionChanged(
        val text: String
    ) : CreateExpenseUiEvent

    data class AmountChanged(
        val text: String
    ) : CreateExpenseUiEvent

    data class PayerSelected(
        val id: String
    ) : CreateExpenseUiEvent

    data class ParticipantToggled(
        val id: String
    ) : CreateExpenseUiEvent

    data object CreateClicked : CreateExpenseUiEvent

    data object BackClicked : CreateExpenseUiEvent
}
