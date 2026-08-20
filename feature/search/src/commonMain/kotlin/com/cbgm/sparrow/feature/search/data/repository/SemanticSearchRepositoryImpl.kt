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
        preparationJob?.cancelAndJoin()
        if (!settingsStorage.isEnabled()) {
            mutableState.value = SemanticSearchState.Disabled
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
                    MessageSearchResult(
                        messageId = message.messageId,
                        conversationId = message.conversationId,
                        conversationName = conversationDisplayName(message.conversationTitle, message.contactName),
                        text = message.text,
                        createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                        matchType = MessageSearchMatchType.SEMANTIC,
                        score = cosineSimilarity(queryEmbedding, EmbeddingCodec.decode(message.embedding))
                    )
                }.sortedByDescending { it.score ?: Float.NEGATIVE_INFINITY }
                .take(limit.coerceAtLeast(1))
                .toList()
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
