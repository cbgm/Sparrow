package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingFeature
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingModelState
import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository

class SetSemanticSearchEnabledUseCase(
    private val localEmbeddingRepository: LocalEmbeddingRepository,
    private val semanticSearchRepository: SemanticSearchRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        localEmbeddingRepository.setFeatureEnabled(LocalEmbeddingFeature.MESSAGE_SEARCH, enabled)

        if (!enabled) {
            semanticSearchRepository.disable()
            return
        }

        if (localEmbeddingRepository.state.value.modelState is LocalEmbeddingModelState.Ready) {
            semanticSearchRepository.prepare()
        }
    }
}
