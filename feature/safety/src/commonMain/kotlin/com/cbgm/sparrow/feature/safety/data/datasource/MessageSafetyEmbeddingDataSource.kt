package com.cbgm.sparrow.feature.safety.data.datasource

import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel
import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.safety.data.model.GeneratedMessageSafetyMlpModel

class MessageSafetyEmbeddingDataSource(
    private val embedder: LocalTextEmbedder
) {
    init {
        check(GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS == LocalEmbeddingModel.OUTPUT_DIMENSIONS) {
            "Safety classifier expects ${GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS} embedding dimensions, " +
                "but the local embedding runtime is configured for ${LocalEmbeddingModel.OUTPUT_DIMENSIONS}"
        }
        check(GeneratedMessageSafetyMlpModel.EMBEDDING_MODEL_SHA256 == LocalEmbeddingModel.MODEL_SHA256) {
            "Safety classifier was trained against a different EmbeddingGemma model"
        }
    }

    suspend fun embed(text: String): FloatArray? {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) return null

        return embedder
            .embed(
                text = normalizedText,
                inputType = EmbeddingInputType.SEMANTIC_SIMILARITY
            )
            .normalizedPrefix(GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS)
    }
}
