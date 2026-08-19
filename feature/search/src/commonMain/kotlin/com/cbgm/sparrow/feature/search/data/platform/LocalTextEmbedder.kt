package com.cbgm.sparrow.feature.search.data.platform

enum class EmbeddingInputType {
    QUERY,
    DOCUMENT
}

interface LocalTextEmbedder {
    suspend fun embed(
        text: String,
        inputType: EmbeddingInputType
    ): FloatArray

    fun close()
}
