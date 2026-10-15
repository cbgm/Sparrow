package com.cbgm.sparrow.feature.expenses.domain.model

data class ExpenseBalance(
    val memberId: String,
    val amountMinor: Long
)

data class ExpenseSettlementSuggestion(
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long
)
