package com.cbgm.sparrow.core.embedding.data.repository

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel
import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalEmbeddingModelManager
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.core.embedding.data.storage.LocalEmbeddingSettingsStorage
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingFeature
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingModelState
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingState
import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalEmbeddingRepositoryImpl(
    private val settingsStorage: LocalEmbeddingSettingsStorage,
    private val modelManager: LocalEmbeddingModelManager,
    private val embedder: LocalTextEmbedder,
    private val applicationScope: ApplicationCoroutineScope
) : LocalEmbeddingRepository {
    private val mutableState = MutableStateFlow(LocalEmbeddingState())
    override val state: StateFlow<LocalEmbeddingState> = mutableState
    private var preparationJob: Job? = null
    private val lifecycleMutex = Mutex()

    override suspend fun initialize(): Unit = lifecycleMutex.withLock {
        val restored =
            try {
                LocalEmbeddingState(
                    semanticSearchEnabled = settingsStorage.isSemanticSearchEnabled(),
                    messageSafetyEnabled = settingsStorage.isMessageSafetyEnabled()
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                mutableState.value =
                    LocalEmbeddingState(
                        modelState =
                            LocalEmbeddingModelState.Failed(
                                throwable.message ?: "Local intelligence settings could not be restored"
                            )
                    )
                return
            }
        mutableState.value = restored

        if (!restored.isModelNeeded) {
            preparationJob?.cancelAndJoin()
            preparationJob = null
            embedder.close()
            modelManager.deleteModel()
            mutableState.value = restored.copy(modelState = LocalEmbeddingModelState.NotNeeded)
            return
        }
        startPreparationIfNeeded()
    }

    override suspend fun setFeatureEnabled(
        feature: LocalEmbeddingFeature,
        enabled: Boolean
    ) = lifecycleMutex.withLock {
        when (feature) {
            LocalEmbeddingFeature.MESSAGE_SEARCH -> settingsStorage.setSemanticSearchEnabled(enabled)
            LocalEmbeddingFeature.MESSAGE_SAFETY -> settingsStorage.setMessageSafetyEnabled(enabled)
        }

        val current = mutableState.value
        val updated =
            when (feature) {
                LocalEmbeddingFeature.MESSAGE_SEARCH -> current.copy(semanticSearchEnabled = enabled)
                LocalEmbeddingFeature.MESSAGE_SAFETY -> current.copy(messageSafetyEnabled = enabled)
            }
        mutableState.value = updated

        if (!updated.isModelNeeded) {
            preparationJob?.cancelAndJoin()
            preparationJob = null
            embedder.close()
            modelManager.deleteModel()
            mutableState.value = updated.copy(modelState = LocalEmbeddingModelState.NotNeeded)
            return
        }

        startPreparationIfNeeded(forceRetry = updated.modelState is LocalEmbeddingModelState.Failed)
    }

    private fun startPreparationIfNeeded(forceRetry: Boolean = false) {
        if (!forceRetry && (mutableState.value.modelState is LocalEmbeddingModelState.Ready || preparationJob?.isActive == true)) {
            return
        }
        preparationJob?.cancel()
        preparationJob = applicationScope.launch { prepareModel() }
    }

    private suspend fun prepareModel() {
        try {
            updateModelState(LocalEmbeddingModelState.Preparing)
            if (!modelManager.isModelReady()) {
                modelManager.downloadAndVerify { progress ->
                    updateModelState(LocalEmbeddingModelState.Downloading(progress))
                }
            }
            validateEmbedder()
            updateModelState(LocalEmbeddingModelState.Ready)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            updateModelState(
                LocalEmbeddingModelState.Failed(
                    throwable.message ?: "Local embedding model could not be prepared"
                )
            )
        }
    }

    private suspend fun validateEmbedder() {
        val embedding =
            embedder.embed(
                text = "local embedding readiness check",
                inputType = EmbeddingInputType.QUERY
            )
        check(embedding.size >= LocalEmbeddingModel.OUTPUT_DIMENSIONS) {
            "Embedding model returned ${embedding.size} dimensions; expected at least ${LocalEmbeddingModel.OUTPUT_DIMENSIONS}"
        }
        check(embedding.all { it.isFinite() }) {
            "Embedding model returned a non-finite embedding"
        }
        check(embedding.normalizedPrefix(LocalEmbeddingModel.OUTPUT_DIMENSIONS).any { it != 0f }) {
            "Embedding model returned an empty embedding"
        }
    }

    private fun updateModelState(modelState: LocalEmbeddingModelState) {
        mutableState.value = mutableState.value.copy(modelState = modelState)
    }
}
