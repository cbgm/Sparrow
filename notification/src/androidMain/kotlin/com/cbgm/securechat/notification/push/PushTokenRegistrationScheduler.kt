package com.cbgm.securechat.notification.push

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cbgm.securechat.notification.work.PushTokenRegistrationWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

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
        const val WORK_NAME = "securechat-push-token-registration"
    }
}
