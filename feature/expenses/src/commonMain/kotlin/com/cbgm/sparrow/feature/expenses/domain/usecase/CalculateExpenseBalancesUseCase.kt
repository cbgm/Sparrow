package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.util.result.safeCall
import com.cbgm.sparrow.feature.expenses.domain.ExpensePayments
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseBalance

/** Each member's balance is the amount they paid minus their share of expenses. */
class CalculateExpenseBalancesUseCase {
    operator fun invoke(
        board: ExpenseBoard,
        expenses: List<Expense>,
        transfers: List<PaymentTransfer> = emptyList()
    ): Result<List<ExpenseBalance>> =
        safeCall(logFailure = false) { calculateBalances(board, expenses, transfers) }

    private fun calculateBalances(board: ExpenseBoard, expenses: List<Expense>, transfers: List<PaymentTransfer>): List<ExpenseBalance> {
        ExpensePolicy.requireValid(board)
        val balancesByMember = mutableMapOf<String, Long>()
        val expenseIds = mutableSetOf<String>()

        for (expense in expenses) {
            validateExpense(board, expense)
            require(expenseIds.add(expense.id)) { "Duplicate expense ID" }

            // The payer receives credit for covering the entire bill.
            balancesByMember.addAmount(expense.paidByMemberId, expense.amountMinor)

            // Each participant owes their assigned share of that bill.
            for (share in expense.allocations) {
                balancesByMember.addAmount(share.memberId, -share.amountMinor)
            }
        }

        ExpensePayments.confirmed(board, transfers).forEach { payment ->
            balancesByMember.addAmount(payment.fromMemberId, payment.amountMinor)
            balancesByMember.addAmount(payment.toMemberId, -payment.amountMinor)
        }

        require(balancesByMember.values.fold(0L, ::addAmountsSafely) == 0L) {
            "Expense balances do not balance"
        }
        return balancesByMember.toSortedMap().map { (memberId, amount) ->
            ExpenseBalance(memberId, amount)
        }
    }

    private fun validateExpense(board: ExpenseBoard, expense: Expense) {
        ExpensePolicy.requireValid(expense)
        require(expense.boardId == board.id) { "Expense belongs to another board" }
        require(expense.currencyCode == board.currencyCode) { "Expense currency differs from its board" }
    }

    private fun MutableMap<String, Long>.addAmount(memberId: String, amount: Long) {
        this[memberId] = addAmountsSafely(this[memberId] ?: 0L, amount)
    }
}

/** Prevent overflow when adding amounts stored in minor currency units. */
internal fun addAmountsSafely(current: Long, addition: Long): Long {
    if (addition > 0L) require(current <= Long.MAX_VALUE - addition) { "Expense amount overflow" }
    if (addition < 0L) require(current >= Long.MIN_VALUE - addition) { "Expense amount overflow" }
    return current + addition
}
