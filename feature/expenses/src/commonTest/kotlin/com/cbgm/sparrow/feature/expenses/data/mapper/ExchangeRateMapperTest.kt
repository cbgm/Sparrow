package com.cbgm.sparrow.feature.expenses.data.mapper

import com.cbgm.sparrow.feature.expenses.data.model.ExchangeRateDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExchangeRateMapperTest {
    @Test
    fun keepsMoneyCurrenciesAndCalculatesCrossRates() {
        val rates =
            listOf(
                ExchangeRateDto("2026-10-14", "EUR", "USD", 1.12),
                ExchangeRateDto("2026-10-14", "EUR", "JPY", 168.0),
                ExchangeRateDto("2026-10-14", "EUR", "XAU", 0.0003),
                ExchangeRateDto("2026-10-13", "EUR", "KYD", 0.93)
            ).toExchangeRatesCacheDto(fetchedAtEpochMilliseconds = 5L).toExchangeRates()

        assertEquals("2026-10-14", rates.date)
        assertTrue("EUR" in rates.currencyCodes)
        assertFalse("XAU" in rates.currencyCodes)
        assertEquals(150.0, rates.rate("USD", "JPY")!!, 0.000001)
        assertEquals(1 / 1.12, rates.rate("USD", "EUR")!!, 0.000001)
    }
}
