package com.cbgm.sparrow.feature.embedding.domain.model

sealed interface LocalEmbeddingModelState {
    data object NotNeeded : LocalEmbeddingModelState

    data object Preparing : LocalEmbeddingModelState

    data class Downloading(
        val progress: Float?
    ) : LocalEmbeddingModelState

    data object Ready : LocalEmbeddingModelState

    data class Failed(
        val message: String
    ) : LocalEmbeddingModelState
}
