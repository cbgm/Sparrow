package com.cbgm.securechat.notification.platform

interface PlatformNotificationRuntime {
    fun initialize()

    fun requestPushTokenRegistration()
}
