package com.cbgm.sparrow.feature.expenses.domain.model

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard

data class ExpenseCreationContext(
    val board: ExpenseBoard,
    val localMemberId: String,
    val members: List<ExpenseMember>,
    val expense: Expense? = null,
    val isLocalAdmin: Boolean = false,
    val recentCurrencyCodes: List<String> = emptyList()
)

data class ExpenseMember(
    val id: String,
    val displayName: String,
    val isLocal: Boolean,
    // UI-only local lookup; never serialized into an expense attachment.
    val avatarContactId: String? = null
)
