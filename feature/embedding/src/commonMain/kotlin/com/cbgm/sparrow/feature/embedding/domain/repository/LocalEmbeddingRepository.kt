package com.cbgm.sparrow.feature.embedding.domain.repository

import com.cbgm.sparrow.feature.embedding.domain.model.LocalEmbeddingFeature
import com.cbgm.sparrow.feature.embedding.domain.model.LocalEmbeddingState
import kotlinx.coroutines.flow.StateFlow

interface LocalEmbeddingRepository {
    val state: StateFlow<LocalEmbeddingState>

    suspend fun initialize()

    suspend fun setFeatureEnabled(
        feature: LocalEmbeddingFeature,
        enabled: Boolean
    )
}
