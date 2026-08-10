package com.cbgm.securechat.platform

import android.content.Intent
import com.cbgm.securechat.di.androidApplicationKoin
import com.cbgm.securechat.notification.platform.SecureChatNotificationIntentHandler

fun consumeAndroidAppIntent(intent: Intent?) {
    val handled =
        androidApplicationKoin()
            .get<SecureChatNotificationIntentHandler>()
            .handle(intent)

    if (handled) {
        intent?.action = null
        intent?.data = null
    }
}
