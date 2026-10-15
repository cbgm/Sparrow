package com.cbgm.sparrow.feature.expenses.presentation.create.model

import com.cbgm.sparrow.feature.expenses.domain.ExpenseMoney
import com.cbgm.sparrow.feature.expenses.presentation.common.mapper.toExpenseAmountLabel
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi

data class ExpenseParticipantUi(
    val id: String,
    val displayName: String,
    val isLocal: Boolean = false,
    val avatarContactId: String? = null
)

data class ExpenseCurrencyUi(
    val code: String,
    val symbol: String,
    val isAvailable: Boolean,
    val isPinned: Boolean = false
) {
    val label: String get() = if (symbol == code) code else "$symbol  $code"
}

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
    val customAmounts: Map<String, String> = emptyMap(),
    val receipt: VisualMediaSelectionUi? = null,
    val scannedAmount: String = "",
    val scannedDescription: String = "",
    val currencyCode: String = "EUR",
    val baseCurrencyCode: String = "EUR",
    val rate: String? = null
) {
    fun isEmpty(): Boolean = description.isBlank() && amount.isBlank()

    val isConverted: Boolean get() = currencyCode != baseCurrencyCode

    val formattedAmount: String
        get() = totalMinor?.toExpenseAmountLabel(currencyCode) ?: "$amount $currencyCode"

    val totalMinor: Long? get() = ExpenseMoney.parseMinor(amount, currencyCode)

    val baseTotalMinor: Long?
        get() {
            val total = totalMinor ?: return null
            if (!isConverted) return total
            val rate = rate ?: return null
            val converted = ExpenseMoney.convert(total, currencyCode, baseCurrencyCode, rate)
            return converted.takeIf { it > 0L }
        }

    /** Sum of the entered custom amounts of the selected participants; invalid entries count as zero. */
    val customAssignedMinor: Long
        get() = participantIds.sumOf { id -> customAmounts[id]?.let(::parseAmount) ?: 0L }

    /** Member shares in minor units, or null while the split is incomplete or does not match the total. */
    fun allocationsMinor(): Map<String, Long>? {
        val total = totalMinor ?: return null
        if (participantIds.isEmpty()) return null
        return when (splitMode) {
            ExpenseSplitModeUi.EQUAL ->
                runCatching { ExpenseMoney.splitEvenly(total, participantIds) }.getOrNull()
            ExpenseSplitModeUi.CUSTOM -> {
                val shares = participantIds.associateWith { id ->
                    customAmounts[id]?.let(::parseAmount) ?: return null
                }
                shares.takeIf { it.values.sum() == total }
            }
        }
    }

    fun baseAllocationsMinor(): Map<String, Long>? {
        val shares = allocationsMinor() ?: return null
        if (!isConverted) return shares
        val baseTotal = baseTotalMinor ?: return null
        return when (splitMode) {
            ExpenseSplitModeUi.EQUAL ->
                runCatching { ExpenseMoney.splitEvenly(baseTotal, participantIds) }.getOrNull()
            ExpenseSplitModeUi.CUSTOM -> {
                val rate = rate ?: return null
                ExpenseMoney.convertShares(shares, baseTotal, currencyCode, baseCurrencyCode, rate)
            }
        }
    }

    fun isValid(memberIds: Set<String>): Boolean =
        description.isNotBlank() && payerId in memberIds && participantIds.isNotEmpty() &&
            participantIds.all { it in memberIds } && baseAllocationsMinor() != null

    private fun parseAmount(text: String): Long? = ExpenseMoney.parseMinor(text, currencyCode)
}

data class CreateExpenseUiState(
    val boardId: String = "",
    val currencyCode: String = "EUR",
    val currencies: List<ExpenseCurrencyUi> = emptyList(),
    val recentCurrencyCodes: List<String> = emptyList(),
    val participants: List<ExpenseParticipantUi> = emptyList(),
    val draft: ExpenseDraftUi = ExpenseDraftUi(),
    val queued: List<ExpenseDraftUi> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val isEditing: Boolean = false,
    val isSplitOpen: Boolean = false
) {
    val canAdd: Boolean get() = !isLoading && !isSending && draft.isValid(participants.mapTo(mutableSetOf()) { it.id })
    val canSend: Boolean get() = !isLoading && !isSending &&
        (queued.isNotEmpty() || canAdd) && (draft.isEmpty() || canAdd)
}
