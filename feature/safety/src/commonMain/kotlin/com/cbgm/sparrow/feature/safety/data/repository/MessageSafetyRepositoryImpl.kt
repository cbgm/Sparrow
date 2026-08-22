package com.cbgm.sparrow.feature.safety.data.repository

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingModelState
import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.data.database.dao.MessageSafetyDao
import com.cbgm.sparrow.feature.safety.data.config.MessageSafetyProcessingConfig
import com.cbgm.sparrow.feature.safety.data.index.MessageSafetyIndexer
import com.cbgm.sparrow.feature.safety.data.mapper.toDomain
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MessageSafetyRepositoryImpl(
    private val dao: MessageSafetyDao,
    private val indexer: MessageSafetyIndexer,
    private val localEmbeddingRepository: LocalEmbeddingRepository,
    private val applicationScope: ApplicationCoroutineScope
) : MessageSafetyRepository {
    private val logger = SparrowLog.withTag("MessageSafetyRepository")
    private val mutableState = MutableStateFlow<MessageSafetyState>(MessageSafetyState.Disabled)
    override val state: StateFlow<MessageSafetyState> = mutableState

    override val assessments: StateFlow<Map<String, MessageSafetyAssessment>> =
        combine(
            dao.observeVisibleAssessments(MessageSafetyProcessingConfig.ANALYZER_VERSION),
            localEmbeddingRepository.state.map { state -> state.messageSafetyEnabled }.distinctUntilChanged()
        ) { stored, enabled ->
            if (!enabled) {
                emptyMap()
            } else {
                stored.associate { assessment -> assessment.messageId to assessment.toDomain() }
            }
        }.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    private var processingJob: Job? = null

    override suspend fun initialize() {
        if (processingJob?.isActive == true) return

        processingJob =
            applicationScope.launch {
                localEmbeddingRepository.state
                    .map { localState ->
                        SafetyRuntimeState(
                            enabled = localState.messageSafetyEnabled,
                            modelState = localState.modelState
                        )
                    }.distinctUntilChanged()
                    .collectLatest(::handleRuntimeState)
            }
    }

    private suspend fun handleRuntimeState(runtimeState: SafetyRuntimeState) {
        if (!runtimeState.enabled) {
            mutableState.value = MessageSafetyState.Disabled
            dao.deleteAllAssessments()
            return
        }

        when (val modelState = runtimeState.modelState) {
            LocalEmbeddingModelState.Ready -> processMessages()
            is LocalEmbeddingModelState.Failed ->
                mutableState.value = MessageSafetyState.Failed(modelState.message)

            LocalEmbeddingModelState.NotNeeded,
            LocalEmbeddingModelState.Preparing,
            is LocalEmbeddingModelState.Downloading ->
                mutableState.value = MessageSafetyState.Preparing
        }
    }

    private suspend fun processMessages() {
        dao.deleteAssessmentsForOtherAnalyzers(MessageSafetyProcessingConfig.ANALYZER_VERSION)

        while (currentCoroutineContext().isActive) {
            val pendingCount = dao.getUnassessedMessageCount(MessageSafetyProcessingConfig.ANALYZER_VERSION)
            if (pendingCount == 0) {
                mutableState.value = MessageSafetyState.Ready
                dao.observeUnassessedMessageCount(MessageSafetyProcessingConfig.ANALYZER_VERSION).first { it > 0 }
                continue
            }

            mutableState.value = MessageSafetyState.Analyzing
            try {
                check(indexer.indexNextBatch() > 0) {
                    "Safety index reported pending messages but did not process a batch"
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logger.warn { "Local message safety analysis failed; retrying" }
                mutableState.value =
                    MessageSafetyState.Failed("Local message safety analysis failed")
                delay(MessageSafetyProcessingConfig.RETRY_DELAY_MILLISECONDS)
                currentCoroutineContext().ensureActive()
            }
        }
    }

    private data class SafetyRuntimeState(
        val enabled: Boolean,
        val modelState: LocalEmbeddingModelState
    )
}
