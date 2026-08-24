package com.cbgm.sparrow.feature.search.domain.usecase

import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.repository.MessageSearchRepository
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository
import kotlinx.coroutines.CancellationException

class SearchMessagesUseCase(
    private val messageSearchRepository: MessageSearchRepository,
    private val semanticSearchRepository: SemanticSearchRepository
) {
    suspend operator fun invoke(
        query: String,
        limit: Int = 30
    ): List<MessageSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        val resultLimit = limit.coerceAtLeast(1)
        val exactResults =
            messageSearchRepository.search(
                query = normalizedQuery,
                limit = resultLimit
            )
        if (exactResults.size >= resultLimit) return exactResults

        val semanticResults =
            try {
                semanticSearchRepository.search(
                    query = normalizedQuery,
                    limit = resultLimit
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
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
