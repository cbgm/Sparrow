package com.cbgm.sparrow.feature.safety.data.classifier

import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel
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

        assertTrue(GeneratedMessageSafetyMlpModel.byReason.keys.none(structuralReasons::contains))
    }

    @Test
    fun generatedModelMatchesPinnedEmbeddingRuntime() {
        assertEquals(LocalEmbeddingModel.OUTPUT_DIMENSIONS, GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS)
        assertEquals(LocalEmbeddingModel.MODEL_FILE_NAME, GeneratedMessageSafetyMlpModel.EMBEDDING_MODEL_ID)
        assertEquals(LocalEmbeddingModel.MODEL_SHA256, GeneratedMessageSafetyMlpModel.EMBEDDING_MODEL_SHA256)
        assertEquals("sentence_similarity", GeneratedMessageSafetyMlpModel.EMBEDDING_INPUT_MODE)
        assertFalse(GeneratedMessageSafetyMlpModel.TRAINING_DATASET_SHA256.isBlank())
    }

    @Test
    fun generatedHeadsHaveValidMlpShapesAndFiniteValues() {
        GeneratedMessageSafetyMlpModel.byReason.values.forEach { head ->
            assertTrue(head.hiddenSize > 0)
            assertEquals(
                GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS * head.hiddenSize,
                head.hiddenWeights.size
            )
            assertEquals(head.hiddenSize, head.hiddenBias.size)
            assertEquals(head.hiddenSize, head.outputWeights.size)
            assertTrue(head.hiddenWeights.all { it.isFinite() })
            assertTrue(head.hiddenBias.all { it.isFinite() })
            assertTrue(head.outputWeights.all { it.isFinite() })
            assertTrue(head.outputBias.isFinite())
            assertTrue(head.threshold.isFinite())
        }
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
