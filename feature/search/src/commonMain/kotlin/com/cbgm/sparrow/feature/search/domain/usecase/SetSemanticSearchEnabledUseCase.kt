package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.embedding.domain.model.LocalEmbeddingFeature
import com.cbgm.sparrow.feature.embedding.domain.model.LocalEmbeddingModelState
import com.cbgm.sparrow.feature.embedding.domain.repository.LocalEmbeddingRepository
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository

class SetSemanticSearchEnabledUseCase(
    private val localEmbeddingRepository: LocalEmbeddingRepository,
    private val semanticSearchRepository: SemanticSearchRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            localEmbeddingRepository.setFeatureEnabled(
                LocalEmbeddingFeature.MESSAGE_SEARCH,
                enabled
            ).getOrThrow()

            if (!enabled) {
                semanticSearchRepository.disable().getOrThrow()
                return@safeSuspendCall
            }

            if (localEmbeddingRepository.state.value.modelState is LocalEmbeddingModelState.Ready) {
                semanticSearchRepository.prepare().getOrThrow()
            }
        }
}
