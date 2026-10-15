package com.cbgm.sparrow.feature.expenses.domain.model

data class ExchangeRates(
    val date: String,
    val fetchedAtEpochMilliseconds: Long,
    val perEuro: Map<String, Double>
) {
    val currencyCodes: List<String> get() = perEuro.keys.sorted()

    fun rate(fromCurrencyCode: String, toCurrencyCode: String): Double? {
        val from = perEuro[fromCurrencyCode] ?: return null
        val to = perEuro[toCurrencyCode] ?: return null
        return to / from
    }
}
