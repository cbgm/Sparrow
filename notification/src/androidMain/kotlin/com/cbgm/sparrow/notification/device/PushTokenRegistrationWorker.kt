package com.cbgm.sparrow.notification.device

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cbgm.sparrow.feature.transport.push.PushPlatform
import com.cbgm.sparrow.notification.domain.usecase.RegisterPushTokenUseCase

class PushTokenRegistrationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val registerPushToken: RegisterPushTokenUseCase
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val token =
            inputData.getString(KEY_TOKEN)
                ?: return Result.failure()

        return registerPushToken(
            token = token,
            platform = PushPlatform.ANDROID
        ).fold(
            onSuccess = {
                Result.success()
            },
            onFailure = {
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
