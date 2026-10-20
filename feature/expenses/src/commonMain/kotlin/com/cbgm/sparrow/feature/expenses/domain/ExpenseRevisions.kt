package com.cbgm.sparrow.feature.expenses.domain

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.expenseId

object ExpenseRevisions {
    fun current(expenses: List<Expense>): List<Expense> =
        expenses
            .groupBy { it.expenseId }
            .values
            .map { revisions -> revisions.maxBy { it.revisionTime() } }
            .filter { it.deletedAtEpochMilliseconds == null }

    private fun Expense.revisionTime(): Long = deletedAtEpochMilliseconds ?: editedAtEpochMilliseconds ?: 0L
}
