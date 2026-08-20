package com.cbgm.sparrow.feature.search.domain.repository

import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import kotlinx.coroutines.flow.StateFlow

interface SemanticSearchRepository {
    val state: StateFlow<SemanticSearchState>

    suspend fun initialize()

    fun setEnabled(enabled: Boolean)

    suspend fun search(
        query: String,
        limit: Int = 30
    ): List<MessageSearchResult>
}
