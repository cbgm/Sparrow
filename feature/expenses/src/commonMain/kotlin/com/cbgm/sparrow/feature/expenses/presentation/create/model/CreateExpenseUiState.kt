package com.cbgm.sparrow.feature.expenses.presentation.create.model

import com.cbgm.sparrow.feature.expenses.domain.ExpenseMoney

data class ExpenseParticipantUi(
    val id: String,
    val displayName: String,
    val isLocal: Boolean = false,
    val avatarContactId: String? = null
)

enum class ExpenseCategoryUi {
    FLIGHT,
    FOOD,
    ACTIVITY,
    PARKING,
    ACCOMMODATION,
    TRANSPORT,
    SHOPPING,
    OTHER
}

data class ExpenseDraftUi(
    val description: String = "",
    val amount: String = "",
    val payerId: String = "",
    val participantIds: Set<String> = emptySet(),
    val category: ExpenseCategoryUi = ExpenseCategoryUi.OTHER
) {
    fun isEmpty(): Boolean = description.isBlank() && amount.isBlank()

    fun formattedAmount(currencyCode: String): String {
        val cents = ExpenseMoney.parseEuroCents(amount) ?: return "$amount $currencyCode"
        val major = cents / 100
        val minor = (cents % 100).toString().padStart(2, '0')
        return if (currencyCode == "EUR") "€$major.$minor" else "$major.$minor $currencyCode"
    }

    fun isValid(memberIds: Set<String>): Boolean {
        val minor = ExpenseMoney.parseEuroCents(amount) ?: return false
        return description.isNotBlank() && payerId in memberIds && participantIds.isNotEmpty() &&
            participantIds.all { it in memberIds } && minor >= participantIds.size
    }
}

data class CreateExpenseUiState(
    val boardId: String = "",
    val currencyCode: String = "EUR",
    val participants: List<ExpenseParticipantUi> = emptyList(),
    val draft: ExpenseDraftUi = ExpenseDraftUi(),
    val queued: List<ExpenseDraftUi> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false
) {
    val canAdd: Boolean get() = !isLoading && !isSending && draft.isValid(participants.mapTo(mutableSetOf()) { it.id })
    val canSend: Boolean get() = !isLoading && !isSending &&
        (queued.isNotEmpty() || canAdd) && (draft.isEmpty() || canAdd)
}
