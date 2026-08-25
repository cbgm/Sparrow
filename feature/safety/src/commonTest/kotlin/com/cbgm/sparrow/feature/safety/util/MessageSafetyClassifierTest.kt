package com.cbgm.sparrow.feature.safety.util

import com.cbgm.sparrow.feature.safety.data.model.GeneratedMessageSafetyMlpModel
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageSafetyClassifierTest {
    private val classifier = MessageSafetyClassifier()

    @Test
    fun zeroEmbeddingDoesNotEmitSemanticReasons() {
        assertTrue(classifier.classify(FloatArray(GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS)).isEmpty())
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
