package com.cbgm.sparrow.notification.device

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
