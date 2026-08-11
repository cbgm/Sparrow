package com.cbgm.securechat.feature.settings.di

import com.cbgm.securechat.core.security.ContactBlocklistRepository
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository
import com.cbgm.securechat.feature.settings.data.repository.DefaultContactBlocklistRepository
import com.cbgm.securechat.feature.settings.data.repository.DefaultDirectIdentitySetupModeRepository
import com.cbgm.securechat.feature.settings.data.repository.LicencesRepositoryImpl
import com.cbgm.securechat.feature.settings.data.repository.SettingsRepositoryImpl
import com.cbgm.securechat.feature.settings.data.storage.registerPlatformSettingsStorage
import com.cbgm.securechat.feature.settings.domain.repository.LicensesRepository
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository
import com.cbgm.securechat.feature.settings.domain.usecase.ClearLocalDataUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetBuildInfoUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetDeveloperEnabledUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetLicenses
import com.cbgm.securechat.feature.settings.domain.usecase.InitAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.ObserveBlockUnknownContactInvites
import com.cbgm.securechat.feature.settings.domain.usecase.ObserveBlockedContactIds
import com.cbgm.securechat.feature.settings.domain.usecase.ObserveDirectIdentitySetupMode
import com.cbgm.securechat.feature.settings.domain.usecase.SetAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetBlockUnknownContactInvites
import com.cbgm.securechat.feature.settings.domain.usecase.SetDeveloperEnabledUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetDirectIdentitySetupMode
import com.cbgm.securechat.feature.settings.presentation.screen.ControlPlaneSettingsViewModel
import com.cbgm.securechat.feature.settings.presentation.screen.SettingsViewModel
import com.cbgm.securechat.feature.settings.presentation.screen.developer.DeveloperMenuViewModel
import com.cbgm.securechat.feature.settings.presentation.screen.disclaimer.DisclaimerViewModel
import com.cbgm.securechat.feature.settings.presentation.screen.licenses.LicensesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule =
    module {
        registerPlatformSettingsStorage()

        single<SettingsRepository> {
            SettingsRepositoryImpl(
                buildInfoProvider = get(),
                settingsStorage = get()
            )
        }

        single<DirectIdentitySetupModeRepository> {
            DefaultDirectIdentitySetupModeRepository(settingsStorage = get())
        }

        single<ContactBlocklistRepository> {
            DefaultContactBlocklistRepository(settingsStorage = get())
        }

        single<LicensesRepository> {
            LicencesRepositoryImpl()
        }

        factory {
            GetAppLanguageUseCase(settingsRepository = get())
        }

        factory {
            GetDeveloperEnabledUseCase(settingsRepository = get())
        }

        factory {
            GetBuildInfoUseCase(settingsRepository = get())
        }

        factory {
            SetAppLanguageUseCase(settingsRepository = get())
        }

        factory {
            InitAppLanguageUseCase(settingsRepository = get())
        }

        factory {
            SetDeveloperEnabledUseCase(settingsRepository = get())
        }

        factory {
            ObserveDirectIdentitySetupMode(repository = get())
        }

        factory {
            SetDirectIdentitySetupMode(repository = get())
        }

        factory {
            ObserveBlockUnknownContactInvites(repository = get())
        }

        factory {
            SetBlockUnknownContactInvites(repository = get())
        }

        factory {
            ObserveBlockedContactIds(repository = get())
        }

        factory {
            ClearLocalDataUseCase(settingsRepository = get())
        }

        factory {
            GetLicenses(repository = get())
        }

        viewModel { DisclaimerViewModel() }

        viewModel {
            SettingsViewModel(
                setAppLanguageUseCase = get(),
                getAppLanguageUseCase = get(),
                getDeveloperEnabledUseCase = get(),
                getBuildInfoUseCase = get(),
                setDeveloperModeEnabledUseCase = get(),
                observeDirectIdentitySetupMode = get(),
                setDirectIdentitySetupMode = get(),
                observeBlockUnknownContactInvites = get(),
                setBlockUnknownContactInvites = get(),
                observeBlockedContactIds = get()
            )
        }

        viewModel {
            ControlPlaneSettingsViewModel(
                configuration = get(),
                statusStore = get(),
                healthMonitor = get(),
                directorySynchronizer = get()
            )
        }

        viewModel {
            DeveloperMenuViewModel(
                clearLocalDataUseCase = get(),
                getBuildInfoUseCase = get(),
                setDeveloperEnabledUseCase = get(),
                transportDiagnosticsProvider = get()
            )
        }

        viewModel {
            LicensesViewModel(getLicenses = get())
        }
    }
