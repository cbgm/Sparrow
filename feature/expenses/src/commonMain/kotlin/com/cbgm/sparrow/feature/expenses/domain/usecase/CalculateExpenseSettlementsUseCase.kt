package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseBalance
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseSettlementSuggestion

/** Deterministic settlement suggestions. This does not initiate a real payment. */
class CalculateExpenseSettlementsUseCase {
    operator fun invoke(balances: List<ExpenseBalance>): List<ExpenseSettlementSuggestion> {
        require(balances.map(ExpenseBalance::memberId).distinct().size == balances.size) { "Duplicate balance member" }
        require(balances.all { it.memberId.isNotBlank() && it.amountMinor != Long.MIN_VALUE }) {
            "Invalid balance"
        }
        require(
            balances.fold(0L) { total, balance ->
                val amount = balance.amountMinor
                require(
                    (amount >= 0L && total <= Long.MAX_VALUE - amount) ||
                        (amount < 0L && total >= Long.MIN_VALUE - amount)
                ) { "Balance sum overflow" }
                total + amount
            } == 0L
        ) { "Balances must sum to zero" }
        val debtors = balances.filter { it.amountMinor < 0L }
            .sortedBy(ExpenseBalance::memberId).map { it.memberId to -it.amountMinor }.toMutableList()
        val creditors = balances.filter { it.amountMinor > 0L }
            .sortedBy(ExpenseBalance::memberId).map { it.memberId to it.amountMinor }.toMutableList()
        val suggestions = mutableListOf<ExpenseSettlementSuggestion>()
        var d = 0
        var c = 0
        while (d < debtors.size && c < creditors.size) {
            val debt = debtors[d]
            val credit = creditors[c]
            val amount = minOf(debt.second, credit.second)
            suggestions += ExpenseSettlementSuggestion(debt.first, credit.first, amount)
            debtors[d] = debt.first to debt.second - amount
            creditors[c] = credit.first to credit.second - amount
            if (debtors[d].second == 0L) d++
            if (creditors[c].second == 0L) c++
        }
        require(d == debtors.size && c == creditors.size) { "Settlement plan is incomplete" }
        return suggestions
    }
}
