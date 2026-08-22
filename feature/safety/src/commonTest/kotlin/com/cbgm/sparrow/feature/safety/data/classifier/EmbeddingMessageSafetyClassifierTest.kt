package com.cbgm.sparrow.feature.safety.data.classifier

import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel
import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmbeddingMessageSafetyClassifierTest {
    @Test
    fun blankTextDoesNotRunEmbeddingInference() = runTest {
        val embedder = RecordingEmbedder(FloatArray(LocalEmbeddingModel.OUTPUT_DIMENSIONS))
        val classifier = EmbeddingMessageSafetyClassifier(embedder)

        assertTrue(classifier.classify("   ").isEmpty())
        assertEquals(0, embedder.calls)
    }

    @Test
    fun usesSemanticSimilarityEmbeddingInput() = runTest {
        val embedder = RecordingEmbedder(FloatArray(LocalEmbeddingModel.OUTPUT_DIMENSIONS))
        val classifier = EmbeddingMessageSafetyClassifier(embedder)

        classifier.classify("ordinary message")

        assertEquals(EmbeddingInputType.SEMANTIC_SIMILARITY, embedder.lastInputType)
        assertEquals("ordinary message", embedder.lastText)
    }

    @Test
    fun trainedHeadCanActivateEachSemanticReason() = runTest {
        GeneratedMessageSafetyLinearModel.byReason.forEach { (reason, head) ->
            val classifier = EmbeddingMessageSafetyClassifier(RecordingEmbedder(head.weights.normalized()))

            val reasons = classifier.classify("test message")

            assertTrue(reason in reasons, "Expected trained head for $reason to activate")
        }
    }

    @Test
    fun zeroEmbeddingDoesNotEmitSemanticReasons() = runTest {
        val classifier =
            EmbeddingMessageSafetyClassifier(
                RecordingEmbedder(FloatArray(LocalEmbeddingModel.OUTPUT_DIMENSIONS))
            )

        assertTrue(classifier.classify("ordinary message").isEmpty())
    }

    @Test
    fun semanticClassifierNeverEmitsStructuralReasons() {
        val structuralReasons =
            setOf(
                MessageSafetyReason.SUSPICIOUS_LINK,
                MessageSafetyReason.LOOKALIKE_DOMAIN,
                MessageSafetyReason.MIXED_SCRIPT_DOMAIN,
                MessageSafetyReason.IP_ADDRESS_LINK,
                MessageSafetyReason.URL_SHORTENER
            )

        assertTrue(GeneratedMessageSafetyLinearModel.byReason.keys.none(structuralReasons::contains))
    }

    @Test
    fun generatedModelMatchesPinnedEmbeddingRuntime() {
        assertEquals(LocalEmbeddingModel.OUTPUT_DIMENSIONS, GeneratedMessageSafetyLinearModel.EMBEDDING_DIMENSIONS)
        assertEquals(LocalEmbeddingModel.MODEL_FILE_NAME, GeneratedMessageSafetyLinearModel.EMBEDDING_MODEL_ID)
        assertEquals(LocalEmbeddingModel.MODEL_SHA256, GeneratedMessageSafetyLinearModel.EMBEDDING_MODEL_SHA256)
        assertEquals("sentence_similarity", GeneratedMessageSafetyLinearModel.EMBEDDING_INPUT_MODE)
        assertFalse(GeneratedMessageSafetyLinearModel.TRAINING_DATASET_SHA256.isBlank())
    }
}

private class RecordingEmbedder(
    private val result: FloatArray
) : LocalTextEmbedder {
    var calls: Int = 0
        private set
    var lastText: String? = null
        private set
    var lastInputType: EmbeddingInputType? = null
        private set

    override suspend fun embed(
        text: String,
        inputType: EmbeddingInputType
    ): FloatArray {
        calls += 1
        lastText = text
        lastInputType = inputType
        return result.copyOf()
    }

    override fun close() = Unit
}

private fun FloatArray.normalized(): FloatArray = normalizedPrefix(size)
