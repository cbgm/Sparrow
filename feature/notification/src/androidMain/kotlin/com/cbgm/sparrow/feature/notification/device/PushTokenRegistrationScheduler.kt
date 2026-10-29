package com.cbgm.sparrow.feature.notification.device

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit

class PushTokenRegistrationScheduler(
    private val context: Context
) {
    fun enqueue(token: String) {
        if (token.isBlank()) {
            return
        }

        val request =
            OneTimeWorkRequestBuilder<PushTokenRegistrationWorker>()
                .setInputData(
                    workDataOf(
                        PushTokenRegistrationWorker.KEY_TOKEN to token
                    )
                ).setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
    }

    /**
     * The push server forgets a token binding 30 days after its last registration. Every app start and
     * foreground already registers again; this renews it for users who do not open the app for weeks.
     */
    fun schedulePeriodicRenewal() {
        val request =
            PeriodicWorkRequestBuilder<PushTokenRenewalWorker>(RENEWAL_INTERVAL_DAYS, TimeUnit.DAYS)
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                RENEWAL_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    @Suppress("DEPRECATION")
    fun enqueueCurrentToken() {
        val firebaseIsInitialized =
            FirebaseApp.getApps(context).isNotEmpty() ||
                FirebaseApp.initializeApp(context) != null

        if (!firebaseIsInitialized) {
            return
        }

        FirebaseMessaging
            .getInstance()
            .token
            .addOnSuccessListener(::enqueue)
    }

    private companion object {
        const val WORK_NAME = "sparrow-push-token-registration"
        const val RENEWAL_WORK_NAME = "sparrow-push-token-renewal"
        const val RENEWAL_INTERVAL_DAYS = 7L
    }
}
