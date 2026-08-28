package com.cbgm.sparrow.feature.search.data.datasource

import com.cbgm.sparrow.core.embedding.data.model.cosineSimilarity
import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.search.data.mapper.EmbeddingCodecMapper
import com.cbgm.sparrow.feature.search.data.model.SemanticSearchIndexConfig

class SemanticSearchEmbeddingDataSource(
    private val embedder: LocalTextEmbedder
) {
    suspend fun embedQuery(query: String): FloatArray =
        embedder
            .embed(query.trim(), EmbeddingInputType.QUERY)
            .normalizedPrefix(SemanticSearchIndexConfig.EMBEDDING_DIMENSIONS)

    fun similarity(
        queryEmbedding: FloatArray,
        storedEmbedding: ByteArray
    ): Float =
        cosineSimilarity(
            queryEmbedding,
            EmbeddingCodecMapper.decode(storedEmbedding)
        )
}
