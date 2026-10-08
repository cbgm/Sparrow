package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseBalance
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseSettlementSuggestion

/** Suggests who pays whom to settle group expenses. No money is transferred here. */
class CalculateExpenseSettlementsUseCase {
    operator fun invoke(balances: List<ExpenseBalance>): Result<List<ExpenseSettlementSuggestion>> =
        runCatching {
            validateBalances(balances)
            suggestTransfers(balances)
        }

    private fun validateBalances(balances: List<ExpenseBalance>) {
        val memberIds = mutableSetOf<String>()
        var total = 0L
        for (balance in balances) {
            require(balance.memberId.isNotBlank()) { "Balance member is missing" }
            require(balance.amountMinor != Long.MIN_VALUE) { "Invalid balance amount" }
            require(memberIds.add(balance.memberId)) { "Duplicate balance member" }
            total = addAmountsSafely(total, balance.amountMinor)
        }
        require(total == 0L) { "Balances must sum to zero" }
    }

    private fun suggestTransfers(balances: List<ExpenseBalance>): List<ExpenseSettlementSuggestion> {
        val debtors = balances.filter { it.amountMinor < 0L }
            .sortedBy(ExpenseBalance::memberId)
            .map { RemainingAmount(it.memberId, -it.amountMinor) }
        val creditors = balances.filter { it.amountMinor > 0L }
            .sortedBy(ExpenseBalance::memberId)
            .map { RemainingAmount(it.memberId, it.amountMinor) }

        val transfers = mutableListOf<ExpenseSettlementSuggestion>()
        var debtorIndex = 0
        var creditorIndex = 0

        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val debtor = debtors[debtorIndex]
            val creditor = creditors[creditorIndex]
            val amountToPay = minOf(debtor.amountMinor, creditor.amountMinor)

            transfers += ExpenseSettlementSuggestion(
                fromMemberId = debtor.memberId,
                toMemberId = creditor.memberId,
                amountMinor = amountToPay
            )
            debtor.amountMinor -= amountToPay
            creditor.amountMinor -= amountToPay

            if (debtor.amountMinor == 0L) debtorIndex++
            if (creditor.amountMinor == 0L) creditorIndex++
        }
        check(debtorIndex == debtors.size && creditorIndex == creditors.size) {
            "Settlement plan is incomplete"
        }
        return transfers
    }

    private data class RemainingAmount(
        val memberId: String,
        var amountMinor: Long
    )
}
