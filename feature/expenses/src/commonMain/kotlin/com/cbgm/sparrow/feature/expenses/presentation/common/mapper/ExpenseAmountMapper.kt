package com.cbgm.sparrow.feature.expenses.presentation.common.mapper

import kotlin.math.absoluteValue

/** Formats minor currency units as a grouped amount, e.g. 108650 EUR -> "€1,086.50". */
fun Long.toExpenseAmountLabel(currencyCode: String): String {
    val absolute = absoluteValue
    val whole = (absolute / 100L).toString().reversed().chunked(3).joinToString(",").reversed()
    val fractional = (absolute % 100L).toString().padStart(2, '0')
    val sign = if (this < 0L) "-" else ""
    return if (currencyCode == "EUR") "$sign€$whole.$fractional" else "$sign$whole.$fractional $currencyCode"
}
