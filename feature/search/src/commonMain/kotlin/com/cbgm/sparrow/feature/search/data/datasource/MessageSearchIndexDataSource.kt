package com.cbgm.sparrow.feature.search.data.datasource

import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.data.database.dao.MessageSearchDao
import com.cbgm.sparrow.data.database.entity.MessageSearchEmbeddingEntity
import com.cbgm.sparrow.feature.search.data.mapper.EmbeddingCodecMapper
import com.cbgm.sparrow.feature.search.data.mapper.toEmbeddingText
import com.cbgm.sparrow.feature.search.data.model.SemanticSearchIndexConfig

class MessageSearchIndexDataSource(
    private val dao: MessageSearchDao,
    private val embedder: LocalTextEmbedder
) {
    suspend fun rebuild(onProgress: (processed: Int, total: Int) -> Unit) {
        dao.deleteEmbeddingsForOtherModels(SemanticSearchIndexConfig.VERSION)
        indexMissing(onProgress)
    }

    suspend fun indexMissing(onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }) {
        val total = dao.getSearchableMessageCount()
        var processed = dao.getIndexedMessageCount(SemanticSearchIndexConfig.VERSION).coerceAtMost(total)
        onProgress(processed, total)

        while (true) {
            val messages =
                dao.getMessagesMissingEmbedding(
                    modelVersion = SemanticSearchIndexConfig.VERSION,
                    limit = SemanticSearchIndexConfig.BATCH_SIZE
                )
            if (messages.isEmpty()) break

            messages.forEach { message ->
                val embedding =
                    embedder
                        .embed(message.toEmbeddingText(), EmbeddingInputType.DOCUMENT)
                        .normalizedPrefix(SemanticSearchIndexConfig.EMBEDDING_DIMENSIONS)
                dao.upsertEmbedding(
                    MessageSearchEmbeddingEntity(
                        messageId = message.messageId,
                        modelVersion = SemanticSearchIndexConfig.VERSION,
                        embedding = EmbeddingCodecMapper.encode(embedding)
                    )
                )
                processed++
                onProgress(processed, total)
            }
        }
    }

    suspend fun clear() = dao.deleteAllEmbeddings()
}
