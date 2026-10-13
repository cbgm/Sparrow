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

enum class ExpenseSplitModeUi {
    EQUAL,
    CUSTOM
}

data class ExpenseDraftUi(
    val description: String = "",
    val amount: String = "",
    val payerId: String = "",
    val participantIds: Set<String> = emptySet(),
    val category: ExpenseCategoryUi = ExpenseCategoryUi.OTHER,
    val splitMode: ExpenseSplitModeUi = ExpenseSplitModeUi.EQUAL,
    // Raw text per participant; only used for custom splits.
    val customAmounts: Map<String, String> = emptyMap()
) {
    fun isEmpty(): Boolean = description.isBlank() && amount.isBlank()

    fun formattedAmount(currencyCode: String): String {
        val cents = ExpenseMoney.parseEuroCents(amount) ?: return "$amount $currencyCode"
        val major = cents / 100
        val minor = (cents % 100).toString().padStart(2, '0')
        return if (currencyCode == "EUR") "€$major.$minor" else "$major.$minor $currencyCode"
    }

    val totalMinor: Long? get() = ExpenseMoney.parseEuroCents(amount)

    /** Sum of the entered custom amounts of the selected participants; invalid entries count as zero. */
    val customAssignedMinor: Long
        get() = participantIds.sumOf { id -> customAmounts[id]?.let(ExpenseMoney::parseEuroCents) ?: 0L }

    /** Member shares in minor units, or null while the split is incomplete or does not match the total. */
    fun allocationsMinor(): Map<String, Long>? {
        val total = totalMinor ?: return null
        if (participantIds.isEmpty()) return null
        return when (splitMode) {
            ExpenseSplitModeUi.EQUAL ->
                runCatching { ExpenseMoney.splitEvenly(total, participantIds) }.getOrNull()
            ExpenseSplitModeUi.CUSTOM -> {
                val shares = participantIds.associateWith { id ->
                    customAmounts[id]?.let(ExpenseMoney::parseEuroCents) ?: return null
                }
                shares.takeIf { it.values.sum() == total }
            }
        }
    }

    fun isValid(memberIds: Set<String>): Boolean =
        description.isNotBlank() && payerId in memberIds && participantIds.isNotEmpty() &&
            participantIds.all { it in memberIds } && allocationsMinor() != null
}

data class CreateExpenseUiState(
    val boardId: String = "",
    val currencyCode: String = "EUR",
    val participants: List<ExpenseParticipantUi> = emptyList(),
    val draft: ExpenseDraftUi = ExpenseDraftUi(),
    val queued: List<ExpenseDraftUi> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val isEditing: Boolean = false
) {
    val canAdd: Boolean get() = !isLoading && !isSending && draft.isValid(participants.mapTo(mutableSetOf()) { it.id })
    val canSend: Boolean get() = !isLoading && !isSending &&
        (queued.isNotEmpty() || canAdd) && (draft.isEmpty() || canAdd)
}
