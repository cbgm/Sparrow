package com.cbgm.sparrow.feature.search.data.datasource

import com.cbgm.sparrow.data.database.dao.MessageSearchDao
import com.cbgm.sparrow.data.database.model.StoredMessageEmbedding
import com.cbgm.sparrow.data.database.model.StoredMessageSearchMatch

class MessageSearchLocalDataSource(
    private val dao: MessageSearchDao
) {
    suspend fun searchExactMessages(
        query: String,
        limit: Int
    ): List<StoredMessageSearchMatch> =
        dao.searchExactMessages(
            query = query,
            limit = limit
        )

    suspend fun getIndexedMessages(modelVersion: Int): List<StoredMessageEmbedding> =
        dao.getIndexedMessages(modelVersion)
}
