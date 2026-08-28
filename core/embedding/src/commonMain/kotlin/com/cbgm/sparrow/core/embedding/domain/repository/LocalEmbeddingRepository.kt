package com.cbgm.sparrow.core.embedding.domain.repository

import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingFeature
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingState
import kotlinx.coroutines.flow.StateFlow

interface LocalEmbeddingRepository {
    val state: StateFlow<LocalEmbeddingState>

    suspend fun initialize()

    suspend fun setFeatureEnabled(
        feature: LocalEmbeddingFeature,
        enabled: Boolean
    )
}
