package com.cbgm.securechat.notification.push

import com.cbgm.securechat.core.logging.SecureChatLog
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class SecureChatFirebaseMessagingService : FirebaseMessagingService() {
    private val logger = SecureChatLog.withTag("SecureChatFCM")

    private val pendingMessageSyncScheduler by inject<PendingMessageSyncScheduler>()

    private val pushTokenRegistrationScheduler by inject<PushTokenRegistrationScheduler>()

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data[KEY_TYPE]

        if (type != TYPE_MESSAGES_AVAILABLE) {
            logger.warn {
                "Ignoring unsupported FCM message type: $type"
            }
            return
        }

        val wakeUpId = message.data[KEY_WAKE_UP_ID]
        if (wakeUpId.isNullOrBlank()) {
            logger.warn {
                "Ignoring messages-available FCM without wake-up ID"
            }
            return
        }

        logger.info {
            "FCM wake-up received; wakeUpId=${wakeUpId.take(LOG_WAKE_UP_ID_LENGTH)}"
        }

        pendingMessageSyncScheduler.enqueue(wakeUpId = wakeUpId)
    }

    override fun onNewToken(token: String) {
        logger.info {
            "FCM token refreshed"
        }

        pushTokenRegistrationScheduler.enqueue(token = token)
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val KEY_WAKE_UP_ID = "wakeUpId"
        const val TYPE_MESSAGES_AVAILABLE = "messages_available"
        const val LOG_WAKE_UP_ID_LENGTH = 8
    }
}
