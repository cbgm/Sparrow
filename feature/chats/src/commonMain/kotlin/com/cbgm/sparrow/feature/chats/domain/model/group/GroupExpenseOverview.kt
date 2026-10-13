package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseBalance
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseSettlementSuggestion

data class GroupExpenseOverview(
    val board: ExpenseBoard,
    val localMemberId: String,
    val isLocalAdmin: Boolean,
    val members: List<GroupExpenseMember>,
    val expenses: List<Expense>,
    val balances: List<ExpenseBalance>,
    val settlements: List<ExpenseSettlementSuggestion>
)
