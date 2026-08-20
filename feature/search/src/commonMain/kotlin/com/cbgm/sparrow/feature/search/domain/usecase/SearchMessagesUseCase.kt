package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.feature.search.domain.repository.MessageSearchRepository

class SearchMessagesUseCase(
    private val repository: MessageSearchRepository
) {
    suspend operator fun invoke(
        query: String,
        limit: Int = 30
    ) = repository.search(query = query, limit = limit)
}
