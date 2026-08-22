package com.cbgm.sparrow.feature.safety.data.classifier

import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel
import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.safety.domain.classifier.MessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import kotlin.math.exp

class EmbeddingMessageSafetyClassifier(
    private val embedder: LocalTextEmbedder
) : MessageSafetyClassifier {
    init {
        check(GeneratedMessageSafetyLinearModel.EMBEDDING_DIMENSIONS == LocalEmbeddingModel.OUTPUT_DIMENSIONS) {
            "Safety classifier expects ${GeneratedMessageSafetyLinearModel.EMBEDDING_DIMENSIONS} embedding dimensions, " +
                "but the local embedding runtime is configured for ${LocalEmbeddingModel.OUTPUT_DIMENSIONS}"
        }
        check(GeneratedMessageSafetyLinearModel.EMBEDDING_MODEL_SHA256 == LocalEmbeddingModel.MODEL_SHA256) {
            "Safety classifier was trained against a different EmbeddingGemma model"
        }
    }

    override suspend fun classify(text: String): Set<MessageSafetyReason> {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) return emptySet()

        val embedding =
            embedder
                .embed(normalizedText, EmbeddingInputType.SEMANTIC_SIMILARITY)
                .normalizedPrefix(GeneratedMessageSafetyLinearModel.EMBEDDING_DIMENSIONS)

        val reasons = linkedSetOf<MessageSafetyReason>()
        GeneratedMessageSafetyLinearModel.byReason.forEach { (reason, head) ->
            if (probability(embedding, head) >= head.threshold) {
                reasons += reason
            }
        }
        return reasons
    }

    private fun probability(
        embedding: FloatArray,
        head: GeneratedMessageSafetyLinearModel.Head
    ): Float {
        require(embedding.size == head.weights.size) {
            "Embedding has ${embedding.size} dimensions, expected ${head.weights.size}"
        }

        var logit = head.bias
        for (index in embedding.indices) {
            logit += embedding[index] * head.weights[index]
        }
        return sigmoid(logit)
    }

    private fun sigmoid(value: Float): Float =
        if (value >= 0f) {
            1f / (1f + exp(-value.toDouble()).toFloat())
        } else {
            val exponential = exp(value.toDouble()).toFloat()
            exponential / (1f + exponential)
        }
}
