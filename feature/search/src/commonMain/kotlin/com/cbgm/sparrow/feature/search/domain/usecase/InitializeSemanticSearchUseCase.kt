package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.embedding.domain.model.LocalEmbeddingModelState
import com.cbgm.sparrow.feature.embedding.domain.repository.LocalEmbeddingRepository
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InitializeSemanticSearchUseCase(
    private val localEmbeddingRepository: LocalEmbeddingRepository,
    private val semanticSearchRepository: SemanticSearchRepository,
    private val applicationScope: ApplicationCoroutineScope
) {
    suspend operator fun invoke(): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            synchronizeCurrentState()
            applicationScope.launch {
                localEmbeddingRepository.state.collectLatest { localState ->
                    when {
                        !localState.semanticSearchEnabled ->
                            semanticSearchRepository.disable().getOrThrow()
                        localState.modelState is LocalEmbeddingModelState.Ready ->
                            semanticSearchRepository.prepare().getOrThrow()
                    }
                }
            }
        }

    private suspend fun synchronizeCurrentState() {
        val localState = localEmbeddingRepository.state.value
        when {
            !localState.semanticSearchEnabled -> semanticSearchRepository.disable().getOrThrow()
            localState.modelState is LocalEmbeddingModelState.Ready ->
                semanticSearchRepository.prepare().getOrThrow()
        }
    }
}
