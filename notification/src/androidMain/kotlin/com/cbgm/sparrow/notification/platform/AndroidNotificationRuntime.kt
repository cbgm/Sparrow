package com.cbgm.sparrow.notification.platform

import com.cbgm.sparrow.notification.push.PushTokenRegistrationScheduler

internal class AndroidNotificationRuntime(
    private val notificationManager: SparrowNotificationManager,
    private val pushTokenRegistrationScheduler: PushTokenRegistrationScheduler
) : PlatformNotificationRuntime {
    override fun initialize() {
        notificationManager.createChannels()
    }

    override fun requestPushTokenRegistration() {
        pushTokenRegistrationScheduler.enqueueCurrentToken()
    }
}
