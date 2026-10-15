package com.cbgm.sparrow.feature.expenses.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard

data class ExpenseOverview(
    val board: ExpenseBoard,
    val localMemberId: String,
    val isLocalAdmin: Boolean,
    val members: List<ExpenseMember>,
    val expenses: List<Expense>,
    val balances: List<ExpenseBalance>,
    val settlements: List<ExpenseSettlementSuggestion>
)
