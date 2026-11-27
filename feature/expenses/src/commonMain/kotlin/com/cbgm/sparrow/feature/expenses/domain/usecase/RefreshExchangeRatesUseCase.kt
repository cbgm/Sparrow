package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.feature.expenses.domain.repository.ExchangeRateRepository
import com.cbgm.sparrow.feature.transport.domain.usecase.GetCurrentNodeBaseUrlUseCase

class RefreshExchangeRatesUseCase(
    private val repository: ExchangeRateRepository,
    private val getCurrentNodeBaseUrl: GetCurrentNodeBaseUrlUseCase
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshIfStale(getCurrentNodeBaseUrl())
}
