package com.cbgm.sparrow.feature.expenses.presentation.currency.mapper

import com.cbgm.sparrow.feature.expenses.domain.ExpenseCurrencies
import com.cbgm.sparrow.feature.expenses.domain.model.ExchangeRates
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCurrencyUi

internal fun ExchangeRates?.toBaseCurrencyUi(): List<ExpenseCurrencyUi> {
    val codes = this?.currencyCodes.orEmpty()
    return (COMMON_CURRENCIES + codes.sorted()).distinct().map { code ->
        ExpenseCurrencyUi(
            code = code,
            symbol = ExpenseCurrencies.symbol(code),
            isAvailable = true,
            isPinned = code in COMMON_CURRENCIES
        )
    }
}

private val COMMON_CURRENCIES = listOf("EUR", "USD", "GBP", "CHF")

internal fun List<ExpenseCurrencyUi>.matching(query: String): List<ExpenseCurrencyUi> {
    val text = query.trim()
    if (text.isEmpty()) return this
    return filter { currency ->
        currency.code.contains(text, ignoreCase = true) ||
            currency.symbol.contains(text, ignoreCase = true)
    }
}
