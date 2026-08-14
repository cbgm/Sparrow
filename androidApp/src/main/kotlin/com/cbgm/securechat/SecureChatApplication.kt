package com.cbgm.securechat

import android.app.Application
import com.cbgm.securechat.di.initializeAndroidDependencyInjection

class SecureChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeAndroidDependencyInjection(application = this)
    }
}
