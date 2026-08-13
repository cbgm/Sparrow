package com.cbgm.securechat.feature.settings.di

import com.cbgm.securechat.core.security.ContactBlocklistRepository
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository
import com.cbgm.securechat.feature.settings.data.datasource.registerPlatformSettingsStorage
import com.cbgm.securechat.feature.settings.data.repository.ContactBlocklistRepositoryImpl
import com.cbgm.securechat.feature.settings.data.repository.DirectIdentitySetupModeRepositoryImpl
import com.cbgm.securechat.feature.settings.data.repository.LicencesRepositoryImpl
import com.cbgm.securechat.feature.settings.data.repository.SettingsRepositoryImpl
import com.cbgm.securechat.feature.settings.domain.repository.LicensesRepository
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository
import com.cbgm.securechat.feature.settings.domain.usecase.ClearLocalDataUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetBuildInfoUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetDeveloperEnabledUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetLicensesUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.InitAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.ObserveBlockUnknownContactInvitesUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.ObserveBlockedContactIdsUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.ObserveDirectIdentitySetupModeUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetBlockUnknownContactInvitesUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetDeveloperEnabledUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetDirectIdentitySetupModeUseCase
import com.cbgm.securechat.feature.settings.presentation.developer.DeveloperMenuViewModel
import com.cbgm.securechat.feature.settings.presentation.disclaimer.DisclaimerViewModel
import com.cbgm.securechat.feature.settings.presentation.licenses.LicensesViewModel
import com.cbgm.securechat.feature.settings.presentation.network.ControlPlaneSettingsViewModel
import com.cbgm.securechat.feature.settings.presentation.overview.SettingsViewModel
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
            DirectIdentitySetupModeRepositoryImpl(settingsStorage = get())
        }

        single<ContactBlocklistRepository> {
            ContactBlocklistRepositoryImpl(settingsStorage = get())
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
            ObserveDirectIdentitySetupModeUseCase(repository = get())
        }

        factory {
            SetDirectIdentitySetupModeUseCase(repository = get())
        }

        factory {
            ObserveBlockUnknownContactInvitesUseCase(repository = get())
        }

        factory {
            SetBlockUnknownContactInvitesUseCase(repository = get())
        }

        factory {
            ObserveBlockedContactIdsUseCase(repository = get())
        }

        factory {
            ClearLocalDataUseCase(settingsRepository = get())
        }

        factory {
            GetLicensesUseCase(repository = get())
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
