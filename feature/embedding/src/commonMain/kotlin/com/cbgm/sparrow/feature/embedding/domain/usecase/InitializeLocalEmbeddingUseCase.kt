package com.cbgm.sparrow.feature.embedding.domain.usecase

import com.cbgm.sparrow.feature.embedding.domain.repository.LocalEmbeddingRepository

class InitializeLocalEmbeddingUseCase(
    private val repository: LocalEmbeddingRepository
) {
    suspend operator fun invoke() = repository.initialize()
}
