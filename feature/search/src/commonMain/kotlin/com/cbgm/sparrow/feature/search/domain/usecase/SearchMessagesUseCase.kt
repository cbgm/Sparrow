package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.repository.MessageSearchRepository
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository

class SearchMessagesUseCase(
    private val messageSearchRepository: MessageSearchRepository,
    private val semanticSearchRepository: SemanticSearchRepository
) {
    suspend operator fun invoke(
        query: String,
        conversationId: String? = null,
        limit: Int = 30
    ): Result<List<MessageSearchResult>> =
        safeSuspendCall(logFailure = false) { search(query, conversationId, limit) }

    private suspend fun search(
        query: String,
        conversationId: String?,
        limit: Int
    ): List<MessageSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        val resultLimit = limit.coerceAtLeast(1)
        val exactResults =
            messageSearchRepository.search(
                query = normalizedQuery,
                conversationId = conversationId,
                limit = resultLimit
            ).getOrThrow()
        if (exactResults.size >= resultLimit) return exactResults

        val semanticResults =
            semanticSearchRepository.search(
                query = normalizedQuery,
                conversationId = conversationId,
                limit = resultLimit
            ).getOrElse { failure ->
                SparrowLog.error("SearchMessagesUseCase", "Semantic search failed; using exact matches", failure)
                return exactResults
            }

        val exactMessageIds = exactResults.mapTo(mutableSetOf()) { it.messageId }
        return exactResults +
            semanticResults
                .asSequence()
                .filterNot { it.messageId in exactMessageIds }
                .take(resultLimit - exactResults.size)
                .toList()
    }
}
