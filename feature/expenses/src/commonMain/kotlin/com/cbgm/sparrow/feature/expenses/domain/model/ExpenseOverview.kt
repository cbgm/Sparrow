package com.cbgm.sparrow.feature.expenses.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer

data class ExpenseOverview(
    val board: ExpenseBoard,
    val localMemberId: String,
    val isLocalAdmin: Boolean,
    val members: List<ExpenseMember>,
    val expenses: List<Expense>,
    val balances: List<ExpenseBalance>,
    val settlements: List<ExpenseSettlementSuggestion>,
    val transfers: List<PaymentTransfer> = emptyList(),
    val paidByMember: Map<String, Long> = emptyMap()
)
