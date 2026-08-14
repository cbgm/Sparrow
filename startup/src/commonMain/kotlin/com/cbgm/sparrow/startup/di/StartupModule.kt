package com.cbgm.sparrow.startup.di

import com.cbgm.sparrow.startup.AppInitializer
import com.cbgm.sparrow.startup.presentation.screen.StartupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val startupModule =
    module {

        single {
            AppInitializer(
                getIdentityStatus = get(),
                recoverIncompleteIdentity = get()
            )
        }

        viewModel {
            StartupViewModel(appInitializer = get())
        }
    }
