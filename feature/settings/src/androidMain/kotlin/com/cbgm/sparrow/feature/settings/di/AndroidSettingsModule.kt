package com.cbgm.sparrow.feature.settings.di

import com.cbgm.sparrow.feature.settings.device.AndroidBuildInfoProvider
import com.cbgm.sparrow.feature.settings.device.BuildInfoProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidSettingsModule =
    module {
        single<BuildInfoProvider> {
            AndroidBuildInfoProvider(
                context = androidContext()
            )
        }
    }
