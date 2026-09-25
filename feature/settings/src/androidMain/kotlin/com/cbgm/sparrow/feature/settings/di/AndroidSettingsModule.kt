package com.cbgm.sparrow.feature.settings.di

import com.cbgm.sparrow.feature.settings.device.AndroidBuildInfoProvider
import com.cbgm.sparrow.feature.settings.device.AndroidSystemLanguageProvider
import com.cbgm.sparrow.feature.settings.device.BuildInfoProvider
import com.cbgm.sparrow.feature.settings.device.SystemLanguageProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidSettingsModule =
    module {
        single<SystemLanguageProvider> { AndroidSystemLanguageProvider() }

        single<BuildInfoProvider> {
            AndroidBuildInfoProvider(
                context = androidContext()
            )
        }
    }
