package com.cbgm.sparrow.feature.expenses.domain.model

/** Aggregated state of a group's active expense board, shown in the pinned bar. */
data class ExpenseSummary(
    val boardId: String,
    val currencyCode: String,
    val expenseCount: Int,
    val totalMinor: Long,
    // Positive when others owe the local member, negative when the local member owes; null if invalid.
    val localBalanceMinor: Long?
)
