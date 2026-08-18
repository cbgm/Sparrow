package com.cbgm.sparrow.di

import android.app.Application
import com.cbgm.sparrow.core.datastore.di.androidDataStoreModule
import com.cbgm.sparrow.data.database.di.androidDatabaseModule
import com.cbgm.sparrow.feature.chats.di.androidChatsModule
import com.cbgm.sparrow.feature.contacts.di.androidContactsModule
import com.cbgm.sparrow.feature.identity.di.androidIdentityStorageModule
import com.cbgm.sparrow.feature.settings.di.androidSettingsModule
import com.cbgm.sparrow.notification.di.notificationAndroidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module

private lateinit var applicationKoin: Koin

private val androidApplicationModules: List<Module> =
    listOf(
        androidDataStoreModule,
        androidDatabaseModule,
        androidIdentityStorageModule,
        androidContactsModule,
        androidChatsModule,
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
