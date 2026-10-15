package com.cbgm.sparrow.feature.expenses.domain

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseSplit
import kotlin.math.pow
import kotlin.math.roundToLong

object ExpenseMoney {
    fun parseMinor(text: String, currencyCode: String): Long? {
        val digits = ExpenseCurrencies.fractionDigits(currencyCode)
        val value = text.trim().replace(',', '.')
        val pieces = value.split('.')
        if (pieces.size !in 1..2 || pieces[0].isEmpty() || pieces[0].any { !it.isDigit() }) return null
        val fractional = pieces.getOrNull(1).orEmpty()
        if (fractional.length > digits || fractional.any { !it.isDigit() }) return null
        val major = pieces[0].toLongOrNull() ?: return null
        val factor = 10L.pow(digits)
        if (major > (Long.MAX_VALUE - factor) / factor) return null
        val fraction =
            if (digits == 0) 0L else fractional.padEnd(digits, '0').toLongOrNull() ?: return null
        return (major * factor + fraction).takeIf { it > 0L }
    }

    fun toInput(amountMinor: Long, currencyCode: String, separator: Char = '.'): String {
        val digits = ExpenseCurrencies.fractionDigits(currencyCode)
        if (digits == 0) return amountMinor.toString()
        val factor = 10L.pow(digits)
        val fraction = (amountMinor % factor).toString().padStart(digits, '0')
        return "${amountMinor / factor}$separator$fraction"
    }

    fun convert(
        amountMinor: Long,
        fromCurrencyCode: String,
        toCurrencyCode: String,
        rate: String
    ): Long {
        val fromFactor = 10.0.pow(ExpenseCurrencies.fractionDigits(fromCurrencyCode))
        val toFactor = 10.0.pow(ExpenseCurrencies.fractionDigits(toCurrencyCode))
        return (amountMinor / fromFactor * rate.toDouble() * toFactor).roundToLong()
    }

    fun convertShares(
        shares: Map<String, Long>,
        totalMinor: Long,
        fromCurrencyCode: String,
        toCurrencyCode: String,
        rate: String
    ): Map<String, Long>? {
        val converted =
            shares.mapValues { (_, amount) ->
                convert(amount, fromCurrencyCode, toCurrencyCode, rate)
            }
        val largest = converted.maxByOrNull { it.value }?.key ?: return null
        val difference = totalMinor - converted.values.sum()
        val adjusted = converted + (largest to converted.getValue(largest) + difference)
        return adjusted.takeIf { result -> result.values.all { it > 0L } }
    }

    fun formatRate(rate: Double): String {
        val scaled = (rate * RATE_SCALE).roundToLong()
        val whole = scaled / RATE_SCALE.toLong()
        val fraction =
            (scaled % RATE_SCALE.toLong()).toString().padStart(RATE_DIGITS, '0').trimEnd('0')
        return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
    }

    /** Every selected participant receives at least one cent; ties use stable member IDs. */
    fun splitEvenly(amountMinor: Long, memberIds: Set<String>): Map<String, Long> =
        ExpenseSplit.evenly(amountMinor, memberIds).associate { it.memberId to it.amountMinor }

    private fun Long.pow(exponent: Int): Long =
        (1..exponent).fold(1L) { result, _ -> result * this }

    private const val RATE_DIGITS = 12
    private const val RATE_SCALE = 1e12
}
