package com.cbgm.sparrow.feature.applock.di

import com.cbgm.sparrow.feature.applock.data.datasource.AppLockSettingsDataSource
import com.cbgm.sparrow.feature.applock.data.repository.AppLockRepositoryImpl
import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository
import com.cbgm.sparrow.feature.applock.domain.usecase.ObserveAppLockEnabledUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.SetAppLockEnabledUseCase
import com.cbgm.sparrow.feature.applock.presentation.AppLockViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appLockModule =
    module {
        single {
            AppLockSettingsDataSource(dataStore = get())
        }

        single<AppLockRepository> {
            AppLockRepositoryImpl(dataSource = get())
        }

        factory {
            ObserveAppLockEnabledUseCase(repository = get())
        }

        factory {
            SetAppLockEnabledUseCase(repository = get())
        }

        viewModel {
            AppLockViewModel(
                observeAppLockEnabled = get()
            )
        }
    }
