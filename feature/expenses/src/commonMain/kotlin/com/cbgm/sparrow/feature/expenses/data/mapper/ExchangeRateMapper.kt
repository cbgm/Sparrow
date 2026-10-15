package com.cbgm.sparrow.feature.expenses.data.mapper

import com.cbgm.sparrow.feature.expenses.data.model.ExchangeRateDto
import com.cbgm.sparrow.feature.expenses.data.model.ExchangeRatesCacheDto
import com.cbgm.sparrow.feature.expenses.domain.ExpenseCurrencies
import com.cbgm.sparrow.feature.expenses.domain.model.ExchangeRates

internal fun List<ExchangeRateDto>.toExchangeRatesCacheDto(
    fetchedAtEpochMilliseconds: Long
): ExchangeRatesCacheDto {
    val rates =
        filter { it.base == EURO && it.rate > 0.0 && ExpenseCurrencies.isMoney(it.quote) }
            .associate { it.quote to it.rate }
    check(rates.isNotEmpty()) { "Exchange rate list is empty" }
    return ExchangeRatesCacheDto(
        date = maxOf { it.date },
        fetchedAtEpochMilliseconds = fetchedAtEpochMilliseconds,
        perEuro = rates + (EURO to 1.0)
    )
}

internal fun ExchangeRatesCacheDto.toExchangeRates(): ExchangeRates =
    ExchangeRates(
        date = date,
        fetchedAtEpochMilliseconds = fetchedAtEpochMilliseconds,
        perEuro = perEuro
    )

private const val EURO = "EUR"
