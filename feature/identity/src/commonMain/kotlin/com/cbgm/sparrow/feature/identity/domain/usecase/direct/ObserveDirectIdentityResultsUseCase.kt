package com.cbgm.sparrow.feature.identity.domain.usecase.direct

import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentityResult
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import kotlinx.coroutines.flow.Flow

class ObserveDirectIdentityResultsUseCase(
    private val repository: DirectIdentityExchangeRepository
) {
    operator fun invoke(): Flow<List<DirectIdentityResult>> = repository.observeResults()
}
