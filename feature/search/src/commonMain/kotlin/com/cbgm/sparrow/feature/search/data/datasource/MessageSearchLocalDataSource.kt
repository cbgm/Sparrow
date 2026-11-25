package com.cbgm.sparrow.feature.search.data.datasource

import com.cbgm.sparrow.data.database.dao.MessageSearchDao
import com.cbgm.sparrow.data.database.model.StoredMessageEmbeddingDto
import com.cbgm.sparrow.data.database.model.StoredMessageSearchMatchDto

class MessageSearchLocalDataSource(
    private val dao: MessageSearchDao
) {
    suspend fun searchExactMessages(
        query: String,
        conversationId: String? = null,
        limit: Int
    ): List<StoredMessageSearchMatchDto> =
        dao.searchExactMessages(
            query = query,
            conversationId = conversationId,
            limit = limit
        )

    suspend fun getIndexedMessages(
        modelVersion: Int,
        conversationId: String? = null
    ): List<StoredMessageEmbeddingDto> =
        dao.getIndexedMessages(
            modelVersion = modelVersion,
            conversationId = conversationId
        )
}
