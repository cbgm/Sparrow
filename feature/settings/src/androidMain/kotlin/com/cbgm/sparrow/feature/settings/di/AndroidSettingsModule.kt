package com.cbgm.sparrow.feature.settings.di

import com.cbgm.sparrow.feature.settings.data.AndroidBuildInfoProvider
import com.cbgm.sparrow.feature.settings.domain.repository.BuildInfoProviderRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidSettingsModule =
    module {
        single<BuildInfoProviderRepository> {
            AndroidBuildInfoProvider(
                context = androidContext()
            )
        }
    }
