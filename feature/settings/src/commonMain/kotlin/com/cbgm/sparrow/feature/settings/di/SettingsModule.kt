package com.cbgm.sparrow.feature.settings.di

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.settings.data.datasource.DeveloperErrorLogStorageDataSource
import com.cbgm.sparrow.feature.settings.data.datasource.SettingsStorage
import com.cbgm.sparrow.feature.settings.data.datasource.SettingsStorageImpl
import com.cbgm.sparrow.feature.settings.data.repository.ContactBlocklistRepositoryImpl
import com.cbgm.sparrow.feature.settings.data.repository.DeveloperErrorLogRepositoryImpl
import com.cbgm.sparrow.feature.settings.data.repository.DirectIdentitySetupModeRepositoryImpl
import com.cbgm.sparrow.feature.settings.data.repository.LicencesRepositoryImpl
import com.cbgm.sparrow.feature.settings.data.repository.SettingsRepositoryImpl
import com.cbgm.sparrow.feature.settings.domain.repository.DeveloperErrorLogRepository
import com.cbgm.sparrow.feature.settings.domain.repository.LicensesRepository
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository
import com.cbgm.sparrow.feature.settings.domain.usecase.ClearDeveloperErrorsUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ClearLocalDataUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.GetAppLanguageUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.GetBuildInfoUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.GetDeveloperEnabledUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.GetLicensesUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.InitAppLanguageUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveBlockUnknownContactInvitesUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveBlockedContactIdsUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveControlPlaneSettingsContextUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveDeveloperErrorsUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveDirectIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveSettingsDomainContextUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetAppLanguageUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetBlockUnknownContactInvitesUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetDeveloperEnabledUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetDirectIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.settings.presentation.developer.DeveloperMenuViewModel
import com.cbgm.sparrow.feature.settings.presentation.disclaimer.DisclaimerViewModel
import com.cbgm.sparrow.feature.settings.presentation.errors.DeveloperErrorLogViewModel
import com.cbgm.sparrow.feature.settings.presentation.licenses.LicensesViewModel
import com.cbgm.sparrow.feature.settings.presentation.network.ControlPlaneSettingsViewModel
import com.cbgm.sparrow.feature.settings.presentation.overview.SettingsViewModel
import com.cbgm.sparrow.feature.settings.presentation.profile.ProfileSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule =
    module {
        single<SettingsStorage> {
            SettingsStorageImpl(dataStore = get())
        }

        single(createdAtStart = true) {
            DeveloperErrorLogStorageDataSource(dataStore = get()).also(SparrowLog::installErrorSink)
        }

        single<DeveloperErrorLogRepository> {
            DeveloperErrorLogRepositoryImpl(dataSource = get())
        }

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
            ObserveSettingsDomainContextUseCase(
                observeDirectIdentitySetupMode = get(),
                observeBlockUnknownContactInvites = get(),
                observeBlockedContactIds = get(),
                observeLocalEmbeddingState = get(),
                observeSemanticSearchState = get(),
                observeMessageSafetyState = get()
            )
        }

        factory {
            ObserveControlPlaneSettingsContextUseCase(
                configuration = get(),
                statusStore = get()
            )
        }

        factory {
            ClearLocalDataUseCase(settingsRepository = get())
        }

        factory {
            GetLicensesUseCase(repository = get())
        }

        factory {
            ObserveDeveloperErrorsUseCase(repository = get())
        }

        factory {
            ClearDeveloperErrorsUseCase(repository = get())
        }

        viewModel { DisclaimerViewModel() }

        viewModel {
            SettingsViewModel(
                setAppLanguageUseCase = get(),
                getAppLanguageUseCase = get(),
                getDeveloperEnabledUseCase = get(),
                getBuildInfoUseCase = get(),
                setDeveloperModeEnabledUseCase = get(),
                observeSettingsDomainContext = get(),
                setDirectIdentitySetupMode = get(),
                setBlockUnknownContactInvites = get(),
                setSemanticSearchEnabled = get(),
                setLocalEmbeddingFeatureEnabled = get()
            )
        }

        viewModel {
            ProfileSettingsViewModel(
                observeLocalProfilePicture = get(),
                setLocalProfilePicture = get(),
                removeLocalProfilePicture = get()
            )
        }

        viewModel {
            ControlPlaneSettingsViewModel(
                savedStateHandle = get(),
                configuration = get(),
                observeControlPlaneSettingsContext = get(),
                healthMonitor = get(),
                directorySynchronizer = get()
            )
        }

        viewModel {
            DeveloperMenuViewModel(
                clearLocalDataUseCase = get(),
                getBuildInfoUseCase = get(),
                observeDeveloperErrorsUseCase = get(),
                setDeveloperEnabledUseCase = get(),
                transportDiagnosticsProvider = get()
            )
        }

        viewModel {
            DeveloperErrorLogViewModel(
                observeDeveloperErrorsUseCase = get(),
                clearDeveloperErrorsUseCase = get()
            )
        }

        viewModel {
            LicensesViewModel(getLicenses = get())
        }
    }
