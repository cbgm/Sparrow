package com.cbgm.sparrow.feature.expenses.presentation.common.mapper

import com.cbgm.sparrow.feature.expenses.domain.ExpenseCurrencies
import kotlin.math.absoluteValue

/** Formats minor currency units as a grouped amount, e.g. 108650 EUR -> "€1,086.50". */
fun Long.toExpenseAmountLabel(currencyCode: String): String {
    val digits = ExpenseCurrencies.fractionDigits(currencyCode)
    val factor = (1..digits).fold(1L) { result, _ -> result * 10L }
    val absolute = absoluteValue
    val whole = (absolute / factor).toString().reversed().chunked(3).joinToString(",").reversed()
    val fractional =
        if (digits == 0) "" else "." + (absolute % factor).toString().padStart(digits, '0')
    val sign = if (this < 0L) "-" else ""
    val symbol = ExpenseCurrencies.symbol(currencyCode)
    return if (symbol != currencyCode) {
        "$sign$symbol$whole$fractional"
    } else {
        "$sign$whole$fractional $currencyCode"
    }
}
