package com.cbgm.sparrow.notification.platform

interface PlatformNotificationRuntime {
    fun initialize()

    fun requestPushTokenRegistration()
}
