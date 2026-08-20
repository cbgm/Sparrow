package com.cbgm.sparrow.core.embedding.domain.usecase

import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository

class ObserveLocalEmbeddingStateUseCase(
    private val repository: LocalEmbeddingRepository
) {
    operator fun invoke() = repository.state
}
