package com.cbgm.securechat.feature.settings.di

import com.cbgm.securechat.feature.settings.data.AndroidBuildInfoProvider
import com.cbgm.securechat.feature.settings.domain.repository.BuildInfoProvider
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
