package com.cbgm.securechat.server.push

import com.google.firebase.FirebaseApp
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
        fun createMessagingOrNull(): FirebaseMessaging? =
            runCatching {
                val app = FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp()
                FirebaseMessaging.getInstance(app)
            }.getOrNull()

        private const val KEY_TYPE = "type"
        private const val KEY_WAKE_UP_ID = "wakeUpId"
        private const val TYPE_MESSAGES_AVAILABLE = "messages_available"
        private const val COLLAPSE_KEY = "securechat-messages"
        private const val PLATFORM_ANDROID = "ANDROID"
        private const val ANALYTICS_LABEL = "securechat_wakeup"
    }
}
