package com.cbgm.sparrow.feature.search.domain.repository

import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult

interface MessageSearchRepository {
    suspend fun search(
        query: String,
        conversationId: String? = null,
        limit: Int = 30
    ): Result<List<MessageSearchResult>>
}
