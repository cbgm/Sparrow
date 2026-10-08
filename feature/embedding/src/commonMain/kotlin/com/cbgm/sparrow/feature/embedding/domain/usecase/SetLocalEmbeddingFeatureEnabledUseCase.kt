package com.cbgm.sparrow.feature.embedding.domain.usecase

import com.cbgm.sparrow.feature.embedding.domain.model.LocalEmbeddingFeature
import com.cbgm.sparrow.feature.embedding.domain.repository.LocalEmbeddingRepository

class SetLocalEmbeddingFeatureEnabledUseCase(
    private val repository: LocalEmbeddingRepository
) {
    suspend operator fun invoke(
        feature: LocalEmbeddingFeature,
        enabled: Boolean
    ) = repository.setFeatureEnabled(feature, enabled)
}
