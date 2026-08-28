package com.cbgm.sparrow.core.embedding.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmbeddingMathTest {
    @Test
    fun normalizedPrefix_truncatesAndNormalizes() {
        val result = floatArrayOf(3f, 4f, 12f).normalizedPrefix(2)

        assertEquals(2, result.size)
        assertEquals(0.6f, result[0], absoluteTolerance = 0.0001f)
        assertEquals(0.8f, result[1], absoluteTolerance = 0.0001f)
    }

    @Test
    fun cosineSimilarity_returnsOneForSameDirection() {
        val first = floatArrayOf(1f, 2f, 3f)
        val second = floatArrayOf(2f, 4f, 6f)

        assertTrue(cosineSimilarity(first, second) > 0.9999f)
    }
}
