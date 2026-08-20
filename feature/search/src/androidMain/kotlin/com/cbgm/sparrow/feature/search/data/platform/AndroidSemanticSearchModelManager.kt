package com.cbgm.sparrow.feature.search.data.platform

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cbgm.sparrow.feature.search.work.SemanticSearchModelDownloadWorker
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.io.File

class AndroidSemanticSearchModelManager(
    context: Context,
    private val modelFiles: AndroidSemanticSearchModelFiles
) : SemanticSearchModelManager {
    private val workManager = WorkManager.getInstance(context)

    override suspend fun isModelReady(): Boolean = modelFiles.isModelReady()

    override suspend fun downloadAndVerify(onProgress: (Float?) -> Unit) {
        if (modelFiles.isModelReady()) {
            onProgress(1f)
            return
        }

        val activeWork =
            workManager
                .getWorkInfosForUniqueWorkFlow(WORK_NAME)
                .first()
                .firstOrNull { !it.state.isFinished }

        val workId =
            activeWork?.id ?: run {
                val request =
                    OneTimeWorkRequestBuilder<SemanticSearchModelDownloadWorker>()
                        .setConstraints(
                            Constraints
                                .Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        ).build()

                workManager.enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
                request.id
            }

        workManager
            .getWorkInfoByIdFlow(workId)
            .filterNotNull()
            .first { workInfo ->
                workInfo.progress
                    .getInt(SemanticSearchModelDownloadWorker.KEY_PROGRESS_PERCENT, -1)
                    .takeIf { it >= 0 }
                    ?.let { onProgress(it / 100f) }

                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        check(modelFiles.isModelReady()) {
                            "Semantic search model worker completed without a verified model"
                        }
                        onProgress(1f)
                        true
                    }

                    WorkInfo.State.FAILED -> {
                        val message =
                            workInfo.outputData.getString(
                                SemanticSearchModelDownloadWorker.KEY_ERROR_MESSAGE
                            ) ?: "Semantic search model download failed"
                        error(message)
                    }

                    WorkInfo.State.CANCELLED -> error("Semantic search model download was cancelled")
                    else -> false
                }
            }
    }

    override suspend fun deleteModel() {
        workManager.cancelUniqueWork(WORK_NAME)
        workManager
            .getWorkInfosForUniqueWorkFlow(WORK_NAME)
            .first { workInfos -> workInfos.none { !it.state.isFinished } }
        modelFiles.deleteAll()
    }

    fun requireVerifiedModelFile(): File = modelFiles.requireVerifiedModelFile()

    companion object {
        const val WORK_NAME = "sparrow-semantic-search-model-download"
    }
}
