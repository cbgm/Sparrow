package com.cbgm.sparrow.feature.expenses.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRateDto(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double
)

@Serializable
data class ExchangeRatesCacheDto(
    val date: String,
    val fetchedAtEpochMilliseconds: Long,
    val perEuro: Map<String, Double>
)
