package com.cbgm.sparrow.feature.search.data.datasource

import com.cbgm.sparrow.data.database.dao.MessageSearchDao
import com.cbgm.sparrow.data.database.model.StoredMessageEmbeddingDto
import com.cbgm.sparrow.data.database.model.StoredMessageSearchMatchDto

class MessageSearchLocalDataSource(
    private val dao: MessageSearchDao
) {
    suspend fun searchExactMessages(
        query: String,
        limit: Int
    ): List<StoredMessageSearchMatchDto> =
        dao.searchExactMessages(
            query = query,
            limit = limit
        )

    suspend fun getIndexedMessages(modelVersion: Int): List<StoredMessageEmbeddingDto> =
        dao.getIndexedMessages(modelVersion)
}
