package com.cbgm.sparrow.core.embedding.domain.model

data class LocalEmbeddingState(
    val semanticSearchEnabled: Boolean = false,
    val messageSafetyEnabled: Boolean = false,
    val modelState: LocalEmbeddingModelState = LocalEmbeddingModelState.NotNeeded
) {
    val isModelNeeded: Boolean
        get() = semanticSearchEnabled || messageSafetyEnabled

    fun isEnabled(feature: LocalEmbeddingFeature): Boolean =
        when (feature) {
            LocalEmbeddingFeature.MESSAGE_SEARCH -> semanticSearchEnabled
            LocalEmbeddingFeature.MESSAGE_SAFETY -> messageSafetyEnabled
        }
}
