package com.cbgm.sparrow.feature.settings.di

import com.cbgm.sparrow.feature.settings.device.AndroidBuildInfoLocalDataSource
import com.cbgm.sparrow.feature.settings.device.AndroidSystemLanguageLocalDataSource
import com.cbgm.sparrow.feature.settings.device.BuildInfoLocalDataSource
import com.cbgm.sparrow.feature.settings.device.SystemLanguageLocalDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidSettingsModule =
    module {
        single<SystemLanguageLocalDataSource> { AndroidSystemLanguageLocalDataSource() }

        single<BuildInfoLocalDataSource> {
            AndroidBuildInfoLocalDataSource(
                context = androidContext()
            )
        }
    }
