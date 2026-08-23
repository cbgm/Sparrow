package com.cbgm.sparrow.core.embedding.data.platform

enum class EmbeddingInputType {
    QUERY,
    DOCUMENT,
    SEMANTIC_SIMILARITY
}

interface LocalTextEmbedder {
    suspend fun embed(
        text: String,
        inputType: EmbeddingInputType
    ): FloatArray

    fun close()
}
