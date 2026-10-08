package com.cbgm.sparrow.feature.expenses.domain

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
    fun splitEvenly(amountMinor: Long, memberIds: Set<String>): Map<String, Long> {
        require(memberIds.isNotEmpty() && memberIds.none(String::isBlank)) { "Select participants" }
        val ids = memberIds.sorted()
        require(amountMinor >= ids.size.toLong()) { "Amount is too small to split" }
        val base = amountMinor / ids.size
        val remainder = (amountMinor % ids.size).toInt()
        return ids.mapIndexed { index, id -> id to (base + if (index < remainder) 1L else 0L) }.toMap()
    }
}
