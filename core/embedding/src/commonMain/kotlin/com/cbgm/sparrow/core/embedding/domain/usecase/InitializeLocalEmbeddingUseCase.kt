package com.cbgm.sparrow.core.embedding.domain.usecase

import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository

class InitializeLocalEmbeddingUseCase(
    private val repository: LocalEmbeddingRepository
) {
    suspend operator fun invoke() = repository.initialize()
}
