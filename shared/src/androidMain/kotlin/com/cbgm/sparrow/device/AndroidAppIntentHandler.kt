package com.cbgm.sparrow.device

import android.content.Intent
import com.cbgm.sparrow.di.androidApplicationKoin
import com.cbgm.sparrow.notification.device.SparrowNotificationIntentHandler

fun consumeAndroidAppIntent(intent: Intent?) {
    val handled =
        androidApplicationKoin()
            .get<SparrowNotificationIntentHandler>()
            .handle(intent)

    if (handled) {
        intent?.action = null
        intent?.data = null
    }
}
