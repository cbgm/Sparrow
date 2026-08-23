package com.cbgm.sparrow.feature.search.data.repository

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.embedding.data.model.cosineSimilarity
import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingFeature
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingModelState
import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository
import com.cbgm.sparrow.data.database.dao.MessageSearchDao
import com.cbgm.sparrow.feature.search.data.embedding.EmbeddingCodec
import com.cbgm.sparrow.feature.search.data.index.MessageSearchIndexer
import com.cbgm.sparrow.feature.search.data.mapper.messageSearchConversationName
import com.cbgm.sparrow.feature.search.data.model.SemanticSearchIndexConfig
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchConversationType
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchMatchType
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SemanticSearchRepositoryImpl(
    private val localEmbeddingRepository: LocalEmbeddingRepository,
    private val indexer: MessageSearchIndexer,
    private val embedder: LocalTextEmbedder,
    private val dao: MessageSearchDao,
    private val applicationScope: ApplicationCoroutineScope
) : SemanticSearchRepository {
    private val mutableState = MutableStateFlow<SemanticSearchState>(SemanticSearchState.Disabled)
    override val state: StateFlow<SemanticSearchState> = mutableState
    private var preparationJob: Job? = null

    override suspend fun initialize() {
        if (!localEmbeddingRepository.state.value.semanticSearchEnabled) {
            preparationJob?.cancelAndJoin()
            preparationJob = null
            mutableState.value = SemanticSearchState.Disabled
            return
        }

        if (mutableState.value is SemanticSearchState.Ready || preparationJob?.isActive == true) return
        preparationJob = applicationScope.launch { prepare() }
    }

    override fun setEnabled(enabled: Boolean) {
        applicationScope.launch {
            preparationJob?.cancelAndJoin()
            preparationJob = null

            localEmbeddingRepository.setFeatureEnabled(LocalEmbeddingFeature.MESSAGE_SEARCH, enabled)
            if (!enabled) {
                indexer.clear()
                mutableState.value = SemanticSearchState.Disabled
                return@launch
            }

            preparationJob = applicationScope.launch { prepare() }
        }
    }

    override suspend fun search(
        query: String,
        limit: Int
    ): List<MessageSearchResult> {
        if (query.isBlank() || state.value !is SemanticSearchState.Ready) return emptyList()
        indexer.indexMissing()
        val queryEmbedding =
            embedder
                .embed(query.trim(), EmbeddingInputType.QUERY)
                .normalizedPrefix(SemanticSearchIndexConfig.EMBEDDING_DIMENSIONS)

        val indexedMessages = dao.getIndexedMessages(SemanticSearchIndexConfig.VERSION)
        return withContext(Dispatchers.Default) {
            indexedMessages
                .asSequence()
                .map { message ->
                    val semanticScore =
                        cosineSimilarity(
                            queryEmbedding,
                            EmbeddingCodec.decode(message.embedding)
                        )
                    val metadataBoost =
                        metadataMatchBoost(
                            query = query,
                            senderName = message.senderName,
                            conversationTitle = message.conversationTitle,
                            contactName = message.contactName
                        )

                    MessageSearchResult(
                        messageId = message.messageId,
                        conversationId = message.conversationId,
                        conversationType = MessageSearchConversationType.valueOf(message.conversationType),
                        contactId = message.contactId,
                        conversationName = messageSearchConversationName(message.conversationTitle, message.contactName),
                        text = message.text,
                        createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                        matchType = MessageSearchMatchType.SEMANTIC,
                        score = (semanticScore + metadataBoost).coerceAtMost(1f)
                    )
                }.filter { result ->
                    (result.score ?: Float.NEGATIVE_INFINITY) >= MIN_SEMANTIC_SIMILARITY
                }.sortedByDescending { it.score ?: Float.NEGATIVE_INFINITY }
                .take(minOf(limit.coerceAtLeast(1), MAX_SEMANTIC_RESULTS))
                .toList()
        }
    }

    private suspend fun prepare() {
        try {
            mutableState.value = SemanticSearchState.Preparing
            awaitSharedModel()
            indexer.rebuild { processed, total ->
                mutableState.value = SemanticSearchState.BuildingIndex(processed, total)
            }
            mutableState.value = SemanticSearchState.Ready
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            mutableState.value =
                SemanticSearchState.Failed(throwable.message ?: "Semantic search setup failed")
        }
    }

    private suspend fun awaitSharedModel() {
        localEmbeddingRepository.state.first { shared ->
            if (!shared.semanticSearchEnabled) throw CancellationException("Semantic search disabled")
            when (val modelState = shared.modelState) {
                LocalEmbeddingModelState.NotNeeded,
                LocalEmbeddingModelState.Preparing -> {
                    mutableState.value = SemanticSearchState.Preparing
                    false
                }

                is LocalEmbeddingModelState.Downloading -> {
                    mutableState.value = SemanticSearchState.DownloadingModel(modelState.progress)
                    false
                }

                LocalEmbeddingModelState.Ready -> true
                is LocalEmbeddingModelState.Failed -> error(modelState.message)
            }
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
