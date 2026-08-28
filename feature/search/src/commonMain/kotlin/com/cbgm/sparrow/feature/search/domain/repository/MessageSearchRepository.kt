package com.cbgm.sparrow.feature.search.domain.repository

import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult

interface MessageSearchRepository {
    suspend fun search(
        query: String,
        limit: Int = 30
    ): List<MessageSearchResult>
}
