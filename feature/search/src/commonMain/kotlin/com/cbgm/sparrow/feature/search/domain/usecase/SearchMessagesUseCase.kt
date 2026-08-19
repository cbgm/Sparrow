package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository

class SearchMessagesUseCase(
    private val repository: SemanticSearchRepository
) {
    suspend operator fun invoke(
        query: String,
        limit: Int = 30
    ) = repository.search(query = query, limit = limit)
}
