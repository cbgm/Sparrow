package com.cbgm.sparrow.feature.notification.device

interface PlatformNotificationRuntime {
    fun initialize()

    fun requestPushTokenRegistration()
}
