package com.cbgm.sparrow.feature.embedding.domain.usecase

import com.cbgm.sparrow.feature.embedding.domain.repository.LocalEmbeddingRepository

class ObserveLocalEmbeddingStateUseCase(
    private val repository: LocalEmbeddingRepository
) {
    operator fun invoke() = repository.state
}
