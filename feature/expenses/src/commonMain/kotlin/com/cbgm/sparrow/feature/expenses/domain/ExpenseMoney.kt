package com.cbgm.sparrow.feature.expenses.domain

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseSplit

/** Exact two-decimal amount parsing for EUR boards. Never converts money through floating point. */
object ExpenseMoney {
    fun parseEuroCents(text: String): Long? {
        val value = text.trim().replace(',', '.')
        val pieces = value.split('.')
        if (pieces.size !in 1..2 || pieces[0].isEmpty() || pieces[0].any { !it.isDigit() }) return null
        val fractional = pieces.getOrNull(1).orEmpty()
        if (fractional.length > 2 || fractional.any { !it.isDigit() }) return null
        val euros = pieces[0].toLongOrNull() ?: return null
        if (euros > (Long.MAX_VALUE - 99) / 100) return null
        val fractionCents = fractional.padEnd(2, '0').toLongOrNull() ?: return null
        val cents = euros * 100 + fractionCents
        return cents.takeIf { it > 0L }
    }

    /** Every selected participant receives at least one cent; ties use stable member IDs. */
    fun splitEvenly(amountMinor: Long, memberIds: Set<String>): Map<String, Long> =
        ExpenseSplit.evenly(amountMinor, memberIds).associate { it.memberId to it.amountMinor }
}
