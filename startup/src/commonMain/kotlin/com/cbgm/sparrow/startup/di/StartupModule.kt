package com.cbgm.sparrow.startup.di

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.startup.domain.usecase.ObserveAppConnectionAvailabilityUseCase
import com.cbgm.sparrow.startup.presentation.start.StartupViewModel
import com.cbgm.sparrow.startup.util.AppInitializer
import com.cbgm.sparrow.startup.util.StartupRuntimeReadiness
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val startupModule =
    module {
        single { StartupRuntimeReadiness() }

        single {
            AppInitializer(
                getIdentityStatus = get(),
                recoverIncompleteIdentity = get(),
                initializeLocalEmbedding = get(),
                initializeSemanticSearch = get(),
                initializeMessageSafety = get(),
                transportConnectionManager = get(),
                runtimeReadiness = get(),
                applicationScope = get<ApplicationCoroutineScope>()
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
