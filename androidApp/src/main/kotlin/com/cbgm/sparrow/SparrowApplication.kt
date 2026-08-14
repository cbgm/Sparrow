package com.cbgm.sparrow

import android.app.Application
import com.cbgm.sparrow.di.initializeAndroidDependencyInjection

class SparrowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeAndroidDependencyInjection(application = this)
    }
}
