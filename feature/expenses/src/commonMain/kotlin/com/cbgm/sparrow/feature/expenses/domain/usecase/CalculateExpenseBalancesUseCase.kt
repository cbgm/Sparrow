package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseBalance

/** Calculates balances using accepted attachments only. Payments are intentionally excluded for now. */
class CalculateExpenseBalancesUseCase {
    operator fun invoke(board: ExpenseBoard, expenses: List<Expense>): List<ExpenseBalance> {
        ExpensePolicy.requireValid(board)
        val balances = mutableMapOf<String, Long>()
        val ids = mutableSetOf<String>()
        expenses.forEach { expense ->
            ExpensePolicy.requireValid(expense)
            require(expense.boardId == board.id) { "Expense belongs to another board" }
            require(expense.currencyCode == board.currencyCode) { "Expense currency differs from its board" }
            require(ids.add(expense.id)) { "Duplicate expense ID" }
            balances[expense.paidByMemberId] = addExact(balances[expense.paidByMemberId] ?: 0L, expense.amountMinor)
            expense.allocations.forEach { allocation ->
                balances[allocation.memberId] = addExact(balances[allocation.memberId] ?: 0L, -allocation.amountMinor)
            }
        }
        require(balances.values.fold(0L, ::addExact) == 0L) { "Expense balances do not balance" }
        return balances.toSortedMap().map { (id, amount) -> ExpenseBalance(id, amount) }
    }

    private fun addExact(a: Long, b: Long): Long {
        require((b >= 0 && a <= Long.MAX_VALUE - b) || (b < 0 && a >= Long.MIN_VALUE - b)) {
            "Expense balance overflow"
        }
        return a + b
    }
}
