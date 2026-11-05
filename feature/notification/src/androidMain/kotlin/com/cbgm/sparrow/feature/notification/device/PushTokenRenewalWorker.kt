package com.cbgm.sparrow.feature.notification.device

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic renewal of the push token binding. It only asks Firebase for the current token and hands it
 * to [PushTokenRegistrationWorker], which signs a fresh registration proof; nothing is stored here.
 */
class PushTokenRenewalWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val pushTokenRegistrationScheduler: PushTokenRegistrationScheduler
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        pushTokenRegistrationScheduler.enqueueCurrentToken()
        return Result.success()
    }
}
