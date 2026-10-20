package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.feature.expenses.domain.repository.ExchangeRateRepository

class RefreshExchangeRatesUseCase(
    private val repository: ExchangeRateRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshIfStale()
}
