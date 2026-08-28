package com.cbgm.sparrow.feature.search.data.repository

import com.cbgm.sparrow.feature.search.data.datasource.MessageSearchLocalDataSource
import com.cbgm.sparrow.feature.search.data.mapper.toMessageSearchResult
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.repository.MessageSearchRepository

class MessageSearchRepositoryImpl(
    private val localDataSource: MessageSearchLocalDataSource
) : MessageSearchRepository {
    override suspend fun search(
        query: String,
        limit: Int
    ): List<MessageSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        return localDataSource
            .searchExactMessages(
                query = normalizedQuery,
                limit = limit.coerceAtLeast(1)
            ).map { it.toMessageSearchResult() }
    }
}
