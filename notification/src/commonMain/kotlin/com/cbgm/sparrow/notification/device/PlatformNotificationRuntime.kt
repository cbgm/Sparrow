package com.cbgm.sparrow.notification.device

interface PlatformNotificationRuntime {
    fun initialize()

    fun requestPushTokenRegistration()
}
