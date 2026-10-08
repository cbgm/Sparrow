package com.cbgm.sparrow.startup.di

import com.cbgm.sparrow.startup.domain.usecase.ObserveAppConnectionAvailabilityUseCase
import com.cbgm.sparrow.startup.presentation.start.StartupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val startupModule =
    module {
        factory {
            ObserveAppConnectionAvailabilityUseCase(
                transportConnectionManager = get()
            )
        }

        viewModel {
            StartupViewModel(
                startupRunner = get()
            )
        }
    }
