package com.cbgm.sparrow.feature.search.data.repository

import com.cbgm.sparrow.data.database.dao.MessageSearchDao
import com.cbgm.sparrow.data.database.model.StoredMessageSearchMatch
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchMatchType
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.repository.MessageSearchRepository
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository
import kotlinx.coroutines.CancellationException

class MessageSearchRepositoryImpl(
    private val dao: MessageSearchDao,
    private val semanticSearchRepository: SemanticSearchRepository
) : MessageSearchRepository {
    override suspend fun search(
        query: String,
        limit: Int
    ): List<MessageSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        val resultLimit = limit.coerceAtLeast(1)
        val exactResults =
            dao.searchExactMessages(
                query = normalizedQuery,
                limit = resultLimit
            ).map(StoredMessageSearchMatch::toDomain)

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

private fun StoredMessageSearchMatch.toDomain(): MessageSearchResult =
    MessageSearchResult(
        messageId = messageId,
        conversationId = conversationId,
        conversationName = conversationDisplayName(conversationTitle, contactName),
        text = text,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        matchType = MessageSearchMatchType.EXACT
    )

internal fun conversationDisplayName(
    conversationTitle: String?,
    contactName: String?
): String? =
    conversationTitle?.trim()?.takeIf(String::isNotEmpty)
        ?: contactName?.trim()?.takeIf(String::isNotEmpty)
