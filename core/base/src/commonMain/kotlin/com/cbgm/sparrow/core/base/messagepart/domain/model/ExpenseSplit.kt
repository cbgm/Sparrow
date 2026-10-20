package com.cbgm.sparrow.core.base.messagepart.domain.model

/** Splits an expense amount across participants. Every participant receives at least one minor unit; ties use stable member IDs. */
object ExpenseSplit {
    fun evenly(amountMinor: Long, memberIds: Set<String>): List<ExpenseAllocation> {
        require(memberIds.isNotEmpty() && memberIds.none(String::isBlank)) { "Select participants" }
        val ids = memberIds.sorted()
        require(amountMinor >= ids.size.toLong()) { "Amount is too small to split" }
        val base = amountMinor / ids.size
        val remainder = (amountMinor % ids.size).toInt()
        return ids.mapIndexed { index, id -> ExpenseAllocation(id, base + if (index < remainder) 1L else 0L) }
    }
}
