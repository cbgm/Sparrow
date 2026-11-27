package com.cbgm.sparrow.feature.applock.di

import com.cbgm.sparrow.feature.applock.data.datasource.AppLockSettingsDataSource
import com.cbgm.sparrow.feature.applock.data.repository.AppLockRepositoryImpl
import com.cbgm.sparrow.feature.applock.domain.model.AppLockSession
import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository
import com.cbgm.sparrow.feature.applock.domain.usecase.ObserveAppLockEnabledUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.ObserveAppLockRequiredUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.ObserveAppLockSettingsUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.RecordAppHiddenUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.RecordAppVisibleUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.SetAppLockEnabledUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.SetAppLockTimeoutUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.UnlockAppUseCase
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

        single { AppLockSession() }

        factory { ObserveAppLockRequiredUseCase(repository = get(), session = get()) }

        factory { ObserveAppLockSettingsUseCase(repository = get()) }

        factory { SetAppLockTimeoutUseCase(repository = get()) }

        factory { UnlockAppUseCase(session = get()) }

        factory { RecordAppHiddenUseCase(repository = get(), session = get()) }

        factory { RecordAppVisibleUseCase(session = get()) }

        viewModel {
            AppLockViewModel(
                observeAppLockRequired = get(),
                unlockApp = get()
            )
        }
    }
