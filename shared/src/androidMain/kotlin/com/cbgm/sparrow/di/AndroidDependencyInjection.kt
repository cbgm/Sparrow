package com.cbgm.sparrow.di

import android.app.Application
import android.content.pm.ApplicationInfo
import com.cbgm.sparrow.BuildKonfig
import com.cbgm.sparrow.data.database.di.androidDatabaseModule
import com.cbgm.sparrow.data.datastore.di.androidDataStoreModule
import com.cbgm.sparrow.feature.attachments.di.androidAttachmentsModule
import com.cbgm.sparrow.feature.contacts.di.androidContactsModule
import com.cbgm.sparrow.feature.embedding.di.androidEmbeddingModule
import com.cbgm.sparrow.feature.identity.di.DEBUG_BUILD_PROPERTY
import com.cbgm.sparrow.feature.identity.di.DEBUG_SMS_CODE_PROPERTY
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
            // Skip and the Firebase test mode exist only in debuggable builds.
            if (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                properties(
                    mapOf(
                        DEBUG_BUILD_PROPERTY to true,
                        DEBUG_SMS_CODE_PROPERTY to BuildKonfig.DEBUG_SMS_CODE
                    )
                )
            }
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
