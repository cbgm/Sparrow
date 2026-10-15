package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.feature.expenses.domain.model.ExchangeRates
import com.cbgm.sparrow.feature.expenses.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow

class ObserveExchangeRatesUseCase(
    private val repository: ExchangeRateRepository
) {
    operator fun invoke(): Flow<ExchangeRates?> = repository.observeRates()
}
