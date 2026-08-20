package com.cbgm.sparrow.feature.search.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cbgm.sparrow.feature.search.data.platform.AndroidSemanticSearchModelDownloader
import com.cbgm.sparrow.feature.search.data.platform.AndroidSemanticSearchModelFiles
import kotlinx.coroutines.CancellationException

class SemanticSearchModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    private val modelDownloader =
        AndroidSemanticSearchModelDownloader(
            modelFiles = AndroidSemanticSearchModelFiles(appContext)
        )

    override suspend fun doWork(): Result =
        try {
            modelDownloader.downloadAndVerify { progress ->
                progress?.let {
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS_PERCENT to
                                (it * 100f).toInt().coerceIn(0, 100)
                        )
                    )
                }
            }
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            if (runAttemptCount >= MAX_RETRY_COUNT) {
                Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to (throwable.message ?: "Model download failed"))
                )
            } else {
                Result.retry()
            }
        }

    companion object {
        const val KEY_PROGRESS_PERCENT = "semantic-search-model-download-progress-percent"
        const val KEY_ERROR_MESSAGE = "semantic-search-model-download-error"

        private const val MAX_RETRY_COUNT = 5
    }
}
