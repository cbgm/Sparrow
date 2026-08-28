package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingModelState
import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class InitializeMessageSafetyUseCase(
    private val repository: MessageSafetyRepository,
    private val localEmbeddingRepository: LocalEmbeddingRepository,
    private val processMessageSafetyBatch: ProcessMessageSafetyBatchUseCase,
    private val applicationScope: ApplicationCoroutineScope
) {
    private val logger = SparrowLog.withTag("InitializeMessageSafetyUseCase")
    private var processingJob: Job? = null

    operator fun invoke() {
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
            repository.updateState(MessageSafetyState.Disabled)
            repository.deleteAllAssessments()
            return
        }

        when (val modelState = runtimeState.modelState) {
            LocalEmbeddingModelState.Ready -> processMessages()
            is LocalEmbeddingModelState.Failed ->
                repository.updateState(MessageSafetyState.Failed(modelState.message))

            LocalEmbeddingModelState.NotNeeded,
            LocalEmbeddingModelState.Preparing,
            is LocalEmbeddingModelState.Downloading ->
                repository.updateState(MessageSafetyState.Preparing)
        }
    }

    private suspend fun processMessages() {
        repository.deleteAssessmentsForOtherAnalyzers()

        while (currentCoroutineContext().isActive) {
            val pendingCount = repository.getUnassessedMessageCount()
            if (pendingCount == 0) {
                repository.updateState(MessageSafetyState.Ready)
                repository.observeUnassessedMessageCount().first { it > 0 }
                continue
            }

            repository.updateState(MessageSafetyState.Analyzing)
            try {
                check(processMessageSafetyBatch(BATCH_SIZE) > 0) {
                    "Safety analysis reported pending messages but did not process a batch"
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logger.warn { "Local message safety analysis failed; retrying" }
                repository.updateState(MessageSafetyState.Failed("Local message safety analysis failed"))
                delay(RETRY_DELAY_MILLISECONDS.milliseconds)
                currentCoroutineContext().ensureActive()
            }
        }
    }

    private data class SafetyRuntimeState(
        val enabled: Boolean,
        val modelState: LocalEmbeddingModelState
    )

    private companion object {
        const val BATCH_SIZE = 8
        const val RETRY_DELAY_MILLISECONDS = 5_000L
    }
}
