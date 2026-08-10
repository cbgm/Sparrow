package com.cbgm.securechat.di

import android.app.Application
import com.cbgm.securechat.data.database.di.androidDatabaseModule
import com.cbgm.securechat.feature.chats.di.androidChatsModule
import com.cbgm.securechat.feature.contacts.di.androidContactsModule
import com.cbgm.securechat.feature.identity.di.androidIdentityStorageModule
import com.cbgm.securechat.feature.settings.di.androidSettingsModule
import com.cbgm.securechat.feature.transport.di.androidTransportModule
import com.cbgm.securechat.notification.di.notificationAndroidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module

private lateinit var applicationKoin: Koin

private val androidApplicationModules: List<Module> =
    listOf(
        androidDatabaseModule,
        androidIdentityStorageModule,
        androidContactsModule,
        androidChatsModule,
        androidTransportModule,
        notificationAndroidModule,
        androidSettingsModule
    )

fun initializeAndroidDependencyInjection(application: Application) {
    applicationKoin =
        startKoin {
            androidLogger()
            androidContext(application)
            workManagerFactory()
            modules(commonApplicationModules + androidApplicationModules)
        }.koin
}

internal fun androidApplicationKoin(): Koin {
    check(::applicationKoin.isInitialized) {
        "Android dependency injection has not been initialized"
    }
    return applicationKoin
}
