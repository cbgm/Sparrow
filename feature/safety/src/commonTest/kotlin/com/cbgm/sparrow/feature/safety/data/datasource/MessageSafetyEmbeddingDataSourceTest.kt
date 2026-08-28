package com.cbgm.sparrow.feature.safety.data.datasource

import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.safety.data.model.GeneratedMessageSafetyMlpModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MessageSafetyEmbeddingDataSourceTest {
    @Test
    fun blankTextDoesNotRunEmbeddingInference() = runTest {
        val embedder = RecordingEmbedder(FloatArray(LocalEmbeddingModel.OUTPUT_DIMENSIONS))
        val dataSource = MessageSafetyEmbeddingDataSource(embedder)

        assertNull(dataSource.embed("   "))
        assertEquals(0, embedder.calls)
    }

    @Test
    fun usesSemanticSimilarityEmbeddingInput() = runTest {
        val embedder = RecordingEmbedder(FloatArray(LocalEmbeddingModel.OUTPUT_DIMENSIONS) { 1f })
        val dataSource = MessageSafetyEmbeddingDataSource(embedder)

        val actual = dataSource.embed("  ordinary message  ")

        assertEquals(EmbeddingInputType.SEMANTIC_SIMILARITY, embedder.lastInputType)
        assertEquals("ordinary message", embedder.lastText)
        assertEquals(GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS, actual?.size)
    }

    @Test
    fun generatedModelMatchesPinnedEmbeddingRuntime() {
        assertEquals(LocalEmbeddingModel.OUTPUT_DIMENSIONS, GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS)
        assertEquals(LocalEmbeddingModel.MODEL_FILE_NAME, GeneratedMessageSafetyMlpModel.EMBEDDING_MODEL_ID)
        assertEquals(LocalEmbeddingModel.MODEL_SHA256, GeneratedMessageSafetyMlpModel.EMBEDDING_MODEL_SHA256)
        assertEquals("sentence_similarity", GeneratedMessageSafetyMlpModel.EMBEDDING_INPUT_MODE)
        assertFalse(GeneratedMessageSafetyMlpModel.TRAINING_DATASET_SHA256.isBlank())
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
