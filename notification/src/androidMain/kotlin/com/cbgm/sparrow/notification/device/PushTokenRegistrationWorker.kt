package com.cbgm.sparrow.notification.device

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.push.PushPlatform
import com.cbgm.sparrow.notification.domain.usecase.RegisterPushTokenUseCase

class PushTokenRegistrationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val registerPushToken: RegisterPushTokenUseCase,
    private val controlPlaneConfiguration: ControlPlaneConfiguration
) : CoroutineWorker(appContext, workerParameters) {
    private val logger = SparrowLog.withTag("PushTokenRegistrationWorker")

    override suspend fun doWork(): Result {
        val token =
            inputData.getString(KEY_TOKEN)
                ?: return Result.failure()

        // Token refresh may also run in a process with no UI initialization.
        return runCatching {
            controlPlaneConfiguration.initialize()
            registerPushToken(
                token = token,
                platform = PushPlatform.ANDROID
            ).getOrThrow()
        }.fold(
            onSuccess = {
                Result.success()
            },
            onFailure = { error ->
                logger.error(error) {
                    "Push-token registration failed; attempt=$runAttemptCount"
                }
                if (runAttemptCount >= MAX_RETRY_COUNT) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        )
    }

    companion object {
        const val KEY_TOKEN = "push-token"

        private const val MAX_RETRY_COUNT = 5
    }
}
