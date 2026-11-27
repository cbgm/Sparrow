package com.cbgm.sparrow.feature.expenses.domain.repository

import com.cbgm.sparrow.feature.expenses.domain.model.ExchangeRates
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun observeRates(): Flow<ExchangeRates?>

    /** [nodeBaseUrl]: the connected node, which fetches the rates; null while none is connected. */
    suspend fun refreshIfStale(nodeBaseUrl: String?): Result<Unit>
}
