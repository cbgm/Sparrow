package com.cbgm.sparrow.feature.embedding.domain.model

data class LocalEmbeddingState(
    val semanticSearchEnabled: Boolean = false,
    val messageSafetyEnabled: Boolean = false,
    val modelState: LocalEmbeddingModelState = LocalEmbeddingModelState.NotNeeded
) {
    val isModelNeeded: Boolean
        get() = semanticSearchEnabled || messageSafetyEnabled
}
