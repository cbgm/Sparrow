package com.cbgm.sparrow.feature.search.data.index

import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.data.database.dao.MessageSearchDao
import com.cbgm.sparrow.data.database.entity.MessageSearchEmbeddingEntity
import com.cbgm.sparrow.feature.search.data.embedding.EmbeddingCodec
import com.cbgm.sparrow.feature.search.data.mapper.toEmbeddingText
import com.cbgm.sparrow.feature.search.data.model.SemanticSearchModel

class MessageSearchIndexer(
    private val dao: MessageSearchDao,
    private val embedder: LocalTextEmbedder
) {
    suspend fun rebuild(onProgress: (processed: Int, total: Int) -> Unit) {
        dao.deleteEmbeddingsForOtherModels(SemanticSearchModel.VERSION)
        indexMissing(onProgress)
    }

    suspend fun indexMissing(onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }) {
        val total = dao.getSearchableMessageCount()
        var processed = dao.getIndexedMessageCount(SemanticSearchModel.VERSION).coerceAtMost(total)
        onProgress(processed, total)

        while (true) {
            val messages =
                dao.getMessagesMissingEmbedding(
                    modelVersion = SemanticSearchModel.VERSION,
                    limit = SemanticSearchModel.INDEX_BATCH_SIZE
                )
            if (messages.isEmpty()) break

            messages.forEach { message ->
                val embedding =
                    embedder
                        .embed(message.toEmbeddingText(), EmbeddingInputType.DOCUMENT)
                        .normalizedPrefix(SemanticSearchModel.OUTPUT_DIMENSIONS)
                dao.upsertEmbedding(
                    MessageSearchEmbeddingEntity(
                        messageId = message.messageId,
                        modelVersion = SemanticSearchModel.VERSION,
                        embedding = EmbeddingCodec.encode(embedding)
                    )
                )
                processed++
                onProgress(processed, total)
            }
        }
    }

    suspend fun clear() = dao.deleteAllEmbeddings()
}
