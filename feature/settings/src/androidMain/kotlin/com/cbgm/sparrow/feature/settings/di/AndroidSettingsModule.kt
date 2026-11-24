package com.cbgm.sparrow.feature.settings.di

import com.cbgm.sparrow.feature.settings.device.AndroidBuildInfoSource
import com.cbgm.sparrow.feature.settings.device.AndroidSystemLanguageSource
import com.cbgm.sparrow.feature.settings.device.BuildInfoSource
import com.cbgm.sparrow.feature.settings.device.SystemLanguageSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidSettingsModule =
    module {
        single<SystemLanguageSource> { AndroidSystemLanguageSource() }

        single<BuildInfoSource> {
            AndroidBuildInfoSource(
                context = androidContext()
            )
        }
    }
