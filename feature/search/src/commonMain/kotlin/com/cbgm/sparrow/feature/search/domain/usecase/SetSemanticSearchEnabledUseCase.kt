package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository

class SetSemanticSearchEnabledUseCase(
    private val repository: SemanticSearchRepository
) {
    operator fun invoke(enabled: Boolean) = repository.setEnabled(enabled)
}
