package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.transport.controlplane.AndroidControlPlaneConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidTransportModule =
    module {
        single {
            AndroidControlPlaneConfiguration(
                context = androidContext()
            )
        }

        single<ControlPlaneConfiguration> {
            get<AndroidControlPlaneConfiguration>()
        }

        single<ControlPlaneStatusStore> {
            get<AndroidControlPlaneConfiguration>()
        }
    }
