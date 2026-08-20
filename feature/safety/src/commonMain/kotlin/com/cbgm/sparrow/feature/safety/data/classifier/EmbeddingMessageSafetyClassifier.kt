package com.cbgm.sparrow.feature.safety.data.classifier

import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel
import com.cbgm.sparrow.core.embedding.data.model.cosineSimilarity
import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.safety.domain.classifier.MessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EmbeddingMessageSafetyClassifier(
    private val embedder: LocalTextEmbedder
) : MessageSafetyClassifier {
    private val mutex = Mutex()
    private var prototypeEmbeddings: Map<MessageSafetyReason, EmbeddedPrototypeSet>? = null

    override suspend fun classify(text: String): Set<MessageSafetyReason> {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) return emptySet()

        val messageEmbedding =
            embedder
                .embed(normalizedText, EmbeddingInputType.SEMANTIC_SIMILARITY)
                .normalizedPrefix(LocalEmbeddingModel.OUTPUT_DIMENSIONS)
        val prototypes = ensurePrototypeEmbeddings()

        return MessageSafetyClassifierPrototypes.byReason.keys.filterTo(linkedSetOf()) { reason ->
            val prototypeSet = prototypes.getValue(reason)
            val positiveSimilarity =
                prototypeSet.positive.maxOf { prototype -> cosineSimilarity(messageEmbedding, prototype) }
            val negativeSimilarity =
                prototypeSet.negative.maxOf { prototype -> cosineSimilarity(messageEmbedding, prototype) }

            MessageSafetyClassifierPolicy
                .threshold(reason)
                .matches(
                    positiveSimilarity = positiveSimilarity,
                    negativeSimilarity = negativeSimilarity
                )
        }
    }

    suspend fun clear() {
        mutex.withLock {
            prototypeEmbeddings = null
        }
    }

    private suspend fun ensurePrototypeEmbeddings(): Map<MessageSafetyReason, EmbeddedPrototypeSet> {
        mutex.withLock { prototypeEmbeddings?.let { return it } }

        val built =
            MessageSafetyClassifierPrototypes.byReason.mapValues { (_, prototypeSet) ->
                EmbeddedPrototypeSet(
                    positive = prototypeSet.positive.map { embedPrototype(it) },
                    negative = prototypeSet.negative.map { embedPrototype(it) }
                )
            }
        return mutex.withLock {
            prototypeEmbeddings ?: built.also { prototypeEmbeddings = it }
        }
    }

    private suspend fun embedPrototype(text: String): FloatArray =
        embedder
            .embed(text, EmbeddingInputType.SEMANTIC_SIMILARITY)
            .normalizedPrefix(LocalEmbeddingModel.OUTPUT_DIMENSIONS)

    private data class EmbeddedPrototypeSet(
        val positive: List<FloatArray>,
        val negative: List<FloatArray>
    )
}
