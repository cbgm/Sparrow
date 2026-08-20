package com.cbgm.sparrow.startup.di

import com.cbgm.sparrow.startup.AppInitializer
import com.cbgm.sparrow.startup.domain.usecase.ObserveAppConnectionAvailabilityUseCase
import com.cbgm.sparrow.startup.presentation.screen.StartupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val startupModule =
    module {

        single {
            AppInitializer(
                getIdentityStatus = get(),
                recoverIncompleteIdentity = get(),
                initializeSemanticSearch = get(),
                transportConnectionManager = get()
            )
        }

        factory {
            ObserveAppConnectionAvailabilityUseCase(
                transportConnectionManager = get()
            )
        }

        viewModel {
            StartupViewModel(appInitializer = get())
        }
    }
