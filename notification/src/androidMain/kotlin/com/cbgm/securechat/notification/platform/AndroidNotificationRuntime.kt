package com.cbgm.securechat.notification.platform

import com.cbgm.securechat.notification.push.PushTokenRegistrationScheduler

internal class AndroidNotificationRuntime(
    private val notificationManager: SecureChatNotificationManager,
    private val pushTokenRegistrationScheduler: PushTokenRegistrationScheduler
) : PlatformNotificationRuntime {
    override fun initialize() {
        notificationManager.createChannels()
    }

    override fun requestPushTokenRegistration() {
        pushTokenRegistrationScheduler.enqueueCurrentToken()
    }
}
