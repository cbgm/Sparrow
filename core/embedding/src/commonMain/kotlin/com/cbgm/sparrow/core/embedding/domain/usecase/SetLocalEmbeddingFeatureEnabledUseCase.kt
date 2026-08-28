package com.cbgm.sparrow.core.embedding.domain.usecase

import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingFeature
import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository

class SetLocalEmbeddingFeatureEnabledUseCase(
    private val repository: LocalEmbeddingRepository
) {
    suspend operator fun invoke(
        feature: LocalEmbeddingFeature,
        enabled: Boolean
    ) = repository.setFeatureEnabled(feature, enabled)
}
