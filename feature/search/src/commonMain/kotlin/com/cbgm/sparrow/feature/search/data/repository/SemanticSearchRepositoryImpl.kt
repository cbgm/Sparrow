package com.cbgm.sparrow.feature.search.data.repository

import com.cbgm.sparrow.feature.search.data.datasource.MessageSearchIndexDataSource
import com.cbgm.sparrow.feature.search.data.datasource.MessageSearchLocalDataSource
import com.cbgm.sparrow.feature.search.data.datasource.SemanticSearchEmbeddingDataSource
import com.cbgm.sparrow.feature.search.data.mapper.toMessageSearchResult
import com.cbgm.sparrow.feature.search.data.model.SemanticSearchIndexConfig
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SemanticSearchRepositoryImpl(
    private val indexDataSource: MessageSearchIndexDataSource,
    private val localDataSource: MessageSearchLocalDataSource,
    private val embeddingDataSource: SemanticSearchEmbeddingDataSource
) : SemanticSearchRepository {
    private val mutableState = MutableStateFlow<SemanticSearchState>(SemanticSearchState.Disabled)
    override val state: StateFlow<SemanticSearchState> = mutableState
    private val lifecycleMutex = Mutex()

    override suspend fun prepare() = lifecycleMutex.withLock {
        if (mutableState.value is SemanticSearchState.Ready) return@withLock

        try {
            mutableState.value = SemanticSearchState.Preparing
            indexDataSource.rebuild { processed, total ->
                mutableState.value = SemanticSearchState.BuildingIndex(processed, total)
            }
            mutableState.value = SemanticSearchState.Ready
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            mutableState.value =
                SemanticSearchState.Failed(
                    throwable.message ?: "Semantic search setup failed"
                )
        }
    }

    override suspend fun disable() = lifecycleMutex.withLock {
        indexDataSource.clear()
        mutableState.value = SemanticSearchState.Disabled
    }

    override suspend fun search(
        query: String,
        limit: Int
    ): List<MessageSearchResult> {
        if (query.isBlank() || state.value !is SemanticSearchState.Ready) return emptyList()

        indexDataSource.indexMissing()
        val queryEmbedding = embeddingDataSource.embedQuery(query)
        val indexedMessages = localDataSource.getIndexedMessages(SemanticSearchIndexConfig.VERSION)

        return withContext(Dispatchers.Default) {
            indexedMessages
                .asSequence()
                .map { message ->
                    val semanticScore =
                        embeddingDataSource.similarity(
                            queryEmbedding = queryEmbedding,
                            storedEmbedding = message.embedding
                        )
                    val metadataBoost =
                        metadataMatchBoost(
                            query = query,
                            senderName = message.senderName,
                            conversationTitle = message.conversationTitle,
                            contactName = message.contactName
                        )
                    message.toMessageSearchResult(
                        score = (semanticScore + metadataBoost).coerceAtMost(1f)
                    )
                }.filter { result ->
                    (result.score ?: Float.NEGATIVE_INFINITY) >= MIN_SEMANTIC_SIMILARITY
                }.sortedByDescending { it.score ?: Float.NEGATIVE_INFINITY }
                .take(minOf(limit.coerceAtLeast(1), MAX_SEMANTIC_RESULTS))
                .toList()
        }
    }

    private fun metadataMatchBoost(
        query: String,
        senderName: String?,
        conversationTitle: String?,
        contactName: String?
    ): Float {
        val queryTokens = searchTokens(query)
        if (queryTokens.isEmpty()) return 0f

        val metadataTokens =
            buildSet {
                senderName?.let { addAll(searchTokens(it)) }
                conversationTitle?.let { addAll(searchTokens(it)) }
                contactName?.let { addAll(searchTokens(it)) }
            }

        return if (queryTokens.any(metadataTokens::contains)) METADATA_MATCH_BOOST else 0f
    }

    private fun searchTokens(value: String): Set<String> =
        value
            .lowercase()
            .split(' ', '\n', '\t', ',', '.', ':', ';', '-', '_', '/', '\\', '(', ')', '[', ']', '{', '}', '?', '!', '"', '\'')
            .asSequence()
            .map(String::trim)
            .filter { it.length >= MIN_METADATA_TOKEN_LENGTH }
            .toSet()

    private companion object {
        const val MIN_SEMANTIC_SIMILARITY = 0.60f
        const val METADATA_MATCH_BOOST = 0.12f
        const val MIN_METADATA_TOKEN_LENGTH = 3
        const val MAX_SEMANTIC_RESULTS = 10
    }
}
