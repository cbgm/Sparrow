package com.cbgm.sparrow.server.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FcmOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import org.slf4j.LoggerFactory

class FirebasePushSender(
    private val messaging: FirebaseMessaging?,
    private val devices: PushDeviceStore,
    private val wakeUps: WakeUpStore
) {
    private val logger = LoggerFactory.getLogger(FirebasePushSender::class.java)

    suspend fun notifyMessagesAvailable(recipientId: String) {
        val firebaseMessaging = messaging ?: return
        val androidDevices = devices.find(recipientId).filter { it.platform == PLATFORM_ANDROID }
        if (androidDevices.isEmpty()) {
            return
        }

        val wakeUpId = wakeUps.create(recipientId)
        androidDevices.forEach { device ->
            // Devices currently register an FCM token, not a Firebase Installation ID.
            // Switching to setFid(device.token) would break working push delivery.
            @Suppress("DEPRECATION")
            val message =
                Message
                    .builder()
                    .setToken(device.token)
                    .putData(KEY_TYPE, TYPE_MESSAGES_AVAILABLE)
                    .putData(KEY_WAKE_UP_ID, wakeUpId)
                    .setFcmOptions(FcmOptions.withAnalyticsLabel(ANALYTICS_LABEL))
                    .setAndroidConfig(
                        AndroidConfig
                            .builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setCollapseKey(COLLAPSE_KEY)
                            .build()
                    ).build()

            try {
                logger.info("FCM wake-up sent: messageId={}", firebaseMessaging.send(message))
            } catch (error: FirebaseMessagingException) {
                if (error.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                    devices.removeToken(device.token)
                }
                logger.error("FCM wake-up failed for recipient {}", recipientId, error)
            }
        }
    }

    companion object {
        fun createMessagingOrNull(): FirebaseMessaging? {
            // No credential mount means push is intentionally disabled. Do not
            // probe Google credentials or prevent encrypted transport startup.
            if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS").isNullOrBlank()) return null
            return runCatching {
                // Each Control Plane administrator can bring a service account
                // from a different Google project. Target Sparrow's *Android*
                // Firebase project, not the service account's source project.
                val projectId = System.getenv("FIREBASE_TARGET_PROJECT_ID")
                    ?.takeIf { it.isNotBlank() }
                    ?: SPARROW_ANDROID_FIREBASE_PROJECT_ID
                val app = FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp(
                    FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setProjectId(projectId)
                        .build()
                )
                FirebaseMessaging.getInstance(app)
            }.onFailure { error ->
                LoggerFactory.getLogger(FirebasePushSender::class.java)
                    .warn("FCM disabled: credentials could not initialize a sender", error)
            }.getOrNull()
        }

        // Verified against androidApp/google-services.json in this source snapshot.
        private const val SPARROW_ANDROID_FIREBASE_PROJECT_ID = "sparrow-a9048"

        private const val KEY_TYPE = "type"
        private const val KEY_WAKE_UP_ID = "wakeUpId"
        private const val TYPE_MESSAGES_AVAILABLE = "messages_available"
        private const val COLLAPSE_KEY = "sparrow-messages"
        private const val PLATFORM_ANDROID = "ANDROID"
        private const val ANALYTICS_LABEL = "sparrow_wakeup"
    }
}
