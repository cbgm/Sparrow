package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingModelState
import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveSemanticSearchStateUseCase(
    private val localEmbeddingRepository: LocalEmbeddingRepository,
    private val semanticSearchRepository: SemanticSearchRepository
) {
    operator fun invoke(): Flow<SemanticSearchState> =
        combine(
            localEmbeddingRepository.state,
            semanticSearchRepository.state
        ) { localState, searchState ->
            if (!localState.semanticSearchEnabled) {
                SemanticSearchState.Disabled
            } else {
                when (val modelState = localState.modelState) {
                    LocalEmbeddingModelState.NotNeeded,
                    LocalEmbeddingModelState.Preparing -> SemanticSearchState.Preparing
                    is LocalEmbeddingModelState.Downloading -> SemanticSearchState.DownloadingModel(modelState.progress)
                    LocalEmbeddingModelState.Ready ->
                        when (searchState) {
                            SemanticSearchState.Disabled -> SemanticSearchState.Preparing
                            else -> searchState
                        }
                    is LocalEmbeddingModelState.Failed -> SemanticSearchState.Failed(modelState.message)
                }
            }
        }.distinctUntilChanged()
}
