package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository

class ObserveSemanticSearchStateUseCase(
    private val repository: SemanticSearchRepository
) {
    operator fun invoke() = repository.state
}
