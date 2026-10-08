package com.cbgm.sparrow.feature.notification.device

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
