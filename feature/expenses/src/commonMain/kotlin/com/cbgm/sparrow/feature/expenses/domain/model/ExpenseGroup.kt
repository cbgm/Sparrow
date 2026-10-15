package com.cbgm.sparrow.feature.expenses.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer

data class ExpenseGroup(
    val groupId: String,
    val board: ExpenseBoard,
    val localMemberId: String?,
    val isLocalAdmin: Boolean,
    val members: List<ExpenseMember>,
    val expenses: List<Expense>,
    val transfers: List<PaymentTransfer> = emptyList()
)
