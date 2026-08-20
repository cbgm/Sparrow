package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository

class InitializeSemanticSearchUseCase(
    private val repository: SemanticSearchRepository
) {
    suspend operator fun invoke() = repository.initialize()
}
