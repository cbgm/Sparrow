package com.cbgm.sparrow.core.embedding.data.platform

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cbgm.sparrow.core.embedding.work.LocalEmbeddingModelDownloadWorker
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.io.File

class AndroidLocalEmbeddingModelManager(
    context: Context,
    private val modelFiles: AndroidLocalEmbeddingModelFiles
) : LocalEmbeddingModelManager {
    private val workManager = WorkManager.getInstance(context)

    override suspend fun isModelReady(): Boolean = modelFiles.isModelReady()

    override suspend fun downloadAndVerify(onProgress: (Float?) -> Unit) {
        if (modelFiles.isModelReady()) {
            onProgress(1f)
            return
        }

        val activeWork =
            workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).first().firstOrNull { !it.state.isFinished }

        val workId =
            activeWork?.id ?: run {
                val request =
                    OneTimeWorkRequestBuilder<LocalEmbeddingModelDownloadWorker>()
                        .setConstraints(
                            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                        ).build()
                workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
                request.id
            }

        workManager.getWorkInfoByIdFlow(workId).filterNotNull().first { workInfo ->
            workInfo.progress.getInt(LocalEmbeddingModelDownloadWorker.KEY_PROGRESS_PERCENT, -1)
                .takeIf { it >= 0 }
                ?.let { onProgress(it / 100f) }

            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> {
                    check(modelFiles.isModelReady()) {
                        "Local embedding model worker completed without a verified model"
                    }
                    onProgress(1f)
                    true
                }

                WorkInfo.State.FAILED -> {
                    error(
                        workInfo.outputData.getString(LocalEmbeddingModelDownloadWorker.KEY_ERROR_MESSAGE)
                            ?: "Local embedding model download failed"
                    )
                }

                WorkInfo.State.CANCELLED -> error("Local embedding model download was cancelled")
                else -> false
            }
        }
    }

    override suspend fun deleteModel() {
        workManager.cancelUniqueWork(WORK_NAME)
        workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME)
            .first { workInfos -> workInfos.none { !it.state.isFinished } }
        modelFiles.deleteAll()
    }

    fun requireVerifiedModelFile(): File = modelFiles.requireVerifiedModelFile()

    companion object {
        // Keep the old unique-work name so an in-flight download from an older build is reused.
        const val WORK_NAME = "sparrow-semantic-search-model-download"
    }
}
