package com.cbgm.sparrow.di

import android.app.Application
import com.cbgm.sparrow.data.database.di.androidDatabaseModule
import com.cbgm.sparrow.data.datastore.di.androidDataStoreModule
import com.cbgm.sparrow.feature.attachments.di.androidAttachmentsModule
import com.cbgm.sparrow.feature.contacts.di.androidContactsModule
import com.cbgm.sparrow.feature.embedding.di.androidEmbeddingModule
import com.cbgm.sparrow.feature.identity.di.androidIdentityStorageModule
import com.cbgm.sparrow.feature.media.di.platformMediaModule
import com.cbgm.sparrow.feature.membership.di.androidMembershipModule
import com.cbgm.sparrow.feature.notification.di.notificationAndroidModule
import com.cbgm.sparrow.feature.ocr.di.platformOcrModule
import com.cbgm.sparrow.feature.settings.di.androidSettingsModule
import com.cbgm.sparrow.feature.voice.di.platformVoiceModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.Koin
import org.koin.core.annotation.KoinViewModelScopeApi
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.option.viewModelScopeFactory

private lateinit var applicationKoin: Koin

private val androidApplicationModules: List<Module> =
    listOf(
        androidDataStoreModule,
        androidEmbeddingModule,
        androidDatabaseModule,
        androidIdentityStorageModule,
        androidContactsModule,
        androidAttachmentsModule,
        platformMediaModule,
        platformOcrModule,
        platformVoiceModule,
        androidMembershipModule,
        notificationAndroidModule,
        androidSettingsModule
    )

@OptIn(KoinViewModelScopeApi::class)
fun initializeAndroidDependencyInjection(application: Application) {
    applicationKoin =
        startKoin {
            options(viewModelScopeFactory())
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
