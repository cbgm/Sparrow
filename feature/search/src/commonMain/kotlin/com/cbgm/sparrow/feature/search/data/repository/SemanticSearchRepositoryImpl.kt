package com.cbgm.sparrow.feature.search.data.repository

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.data.database.dao.MessageSearchDao
import com.cbgm.sparrow.feature.search.data.embedding.EmbeddingCodec
import com.cbgm.sparrow.feature.search.data.embedding.cosineSimilarity
import com.cbgm.sparrow.feature.search.data.embedding.normalizedPrefix
import com.cbgm.sparrow.feature.search.data.index.MessageSearchIndexer
import com.cbgm.sparrow.feature.search.data.model.SemanticSearchModel
import com.cbgm.sparrow.feature.search.data.platform.EmbeddingInputType
import com.cbgm.sparrow.feature.search.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.search.data.platform.SemanticSearchModelManager
import com.cbgm.sparrow.feature.search.data.storage.SemanticSearchSettingsStorage
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchMatchType
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SemanticSearchRepositoryImpl(
    private val settingsStorage: SemanticSearchSettingsStorage,
    private val modelManager: SemanticSearchModelManager,
    private val indexer: MessageSearchIndexer,
    private val embedder: LocalTextEmbedder,
    private val dao: MessageSearchDao,
    private val applicationScope: ApplicationCoroutineScope
) : SemanticSearchRepository {
    private val mutableState = MutableStateFlow<SemanticSearchState>(SemanticSearchState.Disabled)
    override val state: StateFlow<SemanticSearchState> = mutableState
    private var preparationJob: kotlinx.coroutines.Job? = null

    override suspend fun initialize() {
        val enabled =
            try {
                settingsStorage.isEnabled()
            } catch (throwable: Throwable) {
                if (throwable is kotlinx.coroutines.CancellationException) throw throwable
                mutableState.value =
                    SemanticSearchState.Failed(
                        throwable.message ?: "Semantic search initialization failed"
                    )
                return
            }

        if (!enabled) {
            preparationJob?.cancelAndJoin()
            preparationJob = null
            mutableState.value = SemanticSearchState.Disabled
            return
        }

        if (mutableState.value is SemanticSearchState.Ready || preparationJob?.isActive == true) {
            return
        }

        preparationJob = applicationScope.launch { prepare() }
    }

    override fun setEnabled(enabled: Boolean) {
        applicationScope.launch {
            preparationJob?.cancelAndJoin()
            if (!enabled) {
                settingsStorage.setEnabled(false)
                indexer.clear()
                embedder.close()
                modelManager.deleteModel()
                mutableState.value = SemanticSearchState.Disabled
                return@launch
            }

            settingsStorage.setEnabled(true)
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
                .normalizedPrefix(SemanticSearchModel.OUTPUT_DIMENSIONS)

        val indexedMessages = dao.getIndexedMessages(SemanticSearchModel.VERSION)
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
                        conversationName = conversationDisplayName(message.conversationTitle, message.contactName),
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

    private companion object {
        const val MIN_SEMANTIC_SIMILARITY = 0.60f
        const val METADATA_MATCH_BOOST = 0.12f
        const val MIN_METADATA_TOKEN_LENGTH = 3
        const val MAX_SEMANTIC_RESULTS = 10
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

        return if (queryTokens.any(metadataTokens::contains)) {
            METADATA_MATCH_BOOST
        } else {
            0f
        }
    }

    private fun searchTokens(value: String): Set<String> =
        value
            .lowercase()
            .split(
                ' ',
                '\n',
                '\t',
                ',',
                '.',
                ':',
                ';',
                '-',
                '_',
                '/',
                '\\',
                '(',
                ')',
                '[',
                ']',
                '{',
                '}',
                '?',
                '!',
                '"',
                '\''
            )
            .asSequence()
            .map(String::trim)
            .filter { it.length >= MIN_METADATA_TOKEN_LENGTH }
            .toSet()

    private suspend fun validateEmbedder() {
        val embedding =
            embedder.embed(
                text = "semantic search readiness check",
                inputType = EmbeddingInputType.QUERY
            )

        check(embedding.size >= SemanticSearchModel.OUTPUT_DIMENSIONS) {
            "Semantic search model returned ${embedding.size} dimensions; expected at least ${SemanticSearchModel.OUTPUT_DIMENSIONS}"
        }
        check(embedding.all { it.isFinite() }) {
            "Semantic search model returned a non-finite embedding"
        }

        val normalized = embedding.normalizedPrefix(SemanticSearchModel.OUTPUT_DIMENSIONS)
        check(normalized.any { it != 0f }) {
            "Semantic search model returned an empty embedding"
        }
    }

    private suspend fun prepare() {
        try {
            mutableState.value = SemanticSearchState.Preparing
            if (!modelManager.isModelReady()) {
                modelManager.downloadAndVerify { progress ->
                    mutableState.value = SemanticSearchState.DownloadingModel(progress)
                }
            }
            validateEmbedder()
            indexer.rebuild { processed, total ->
                mutableState.value = SemanticSearchState.BuildingIndex(processed, total)
            }
            mutableState.value = SemanticSearchState.Ready
        } catch (throwable: Throwable) {
            if (throwable is kotlinx.coroutines.CancellationException) throw throwable
            mutableState.value = SemanticSearchState.Failed(throwable.message ?: "Semantic search setup failed")
        }
    }
}
