package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import kotlinx.coroutines.flow.Flow

class ObserveIdentityResultsUseCase(
    private val repository: IdentityExchangeRepository
) {
    operator fun invoke(): Flow<List<IdentityResult>> = repository.observeResults()
}
