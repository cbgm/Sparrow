package com.cbgm.sparrow.feature.expenses.domain.repository

import com.cbgm.sparrow.feature.expenses.domain.model.ExchangeRates
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun observeRates(): Flow<ExchangeRates?>

    suspend fun refreshIfStale(): Result<Unit>
}
