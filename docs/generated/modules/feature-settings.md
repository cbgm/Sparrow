# `:feature:settings`

Source directory: `feature/settings`

## Direct project dependencies

- `:core`
- `:data:datastore`
- `:core:embedding`
- `:core:ui`
- `:feature:identity`
- `:feature:contacts`
- `:feature:avatar`
- `:feature:autoreply`
- `:feature:voice`
- `:feature:search`
- `:feature:safety`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidBuildInfoProvider` | `class` | `androidMain` | `feature/settings/src/androidMain/kotlin/com/cbgm/sparrow/feature/settings/device/AndroidBuildInfoProvider.kt` |
| `AndroidSystemLanguageProvider` | `class` | `androidMain` | `feature/settings/src/androidMain/kotlin/com/cbgm/sparrow/feature/settings/device/AndroidSystemLanguageProvider.kt` |
| `DeveloperErrorLogStorageDataSource` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/DeveloperErrorLogStorageDataSource.kt` |
| `InMemorySettingsStorage` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/InMemorySettingsStorage.kt` |
| `SettingsStorage` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/SettingsStorage.kt` |
| `SettingsStorageImpl` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/SettingsStorageImpl.kt` |
| `DeveloperErrorDto` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/model/DeveloperErrorDto.kt` |
| `ContactBlocklistRepositoryImpl` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/ContactBlocklistRepositoryImpl.kt` |
| `DeveloperErrorLogRepositoryImpl` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/DeveloperErrorLogRepositoryImpl.kt` |
| `DirectIdentitySetupModeRepositoryImpl` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/DirectIdentitySetupModeRepositoryImpl.kt` |
| `LicencesRepositoryImpl` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/LicencesRepositoryImpl.kt` |
| `SettingsRepositoryImpl` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/SettingsRepositoryImpl.kt` |
| `BuildInfoProvider` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/device/BuildInfoProvider.kt` |
| `SystemLanguageProvider` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/device/SystemLanguageProvider.kt` |
| `BuildInfo` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/BuildInfo.kt` |
| `ControlPlaneSettingsContext` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/ControlPlaneSettingsContext.kt` |
| `DeveloperError` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/DeveloperError.kt` |
| `DisclaimerContent` | `object` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/DisclaimerContent.kt` |
| `SettingsDomainContext` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/SettingsDomainContext.kt` |
| `DeveloperErrorLogRepository` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/repository/DeveloperErrorLogRepository.kt` |
| `LicensesRepository` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/repository/LicensesRepository.kt` |
| `SettingsRepository` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/repository/SettingsRepository.kt` |
| `ClearDeveloperErrorsUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ClearDeveloperErrorsUseCase.kt` |
| `ClearLocalDataUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ClearLocalDataUseCase.kt` |
| `GetAppLanguageUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetAppLanguageUseCase.kt` |
| `GetBuildInfoUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetBuildInfoUseCase.kt` |
| `GetDeveloperEnabledUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetDeveloperEnabledUseCase.kt` |
| `GetLicensesUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetLicensesUseCase.kt` |
| `InitAppLanguageUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/InitAppLanguageUseCase.kt` |
| `ObserveBlockUnknownContactInvitesUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveBlockUnknownContactInvitesUseCase.kt` |
| `ObserveBlockedContactIdsUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveBlockedContactIdsUseCase.kt` |
| `ObserveControlPlaneSettingsContextUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveControlPlaneSettingsContextUseCase.kt` |
| `ObserveDeveloperErrorsUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveDeveloperErrorsUseCase.kt` |
| `ObserveDirectIdentitySetupModeUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveDirectIdentitySetupModeUseCase.kt` |
| `ObserveSettingsDomainContextUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveSettingsDomainContextUseCase.kt` |
| `SetAppLanguageUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetAppLanguageUseCase.kt` |
| `SetBlockUnknownContactInvitesUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetBlockUnknownContactInvitesUseCase.kt` |
| `SetDeveloperEnabledUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetDeveloperEnabledUseCase.kt` |
| `SetDirectIdentitySetupModeUseCase` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetDirectIdentitySetupModeUseCase.kt` |
| `DeveloperMenuViewModel` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/DeveloperMenuViewModel.kt` |
| `NodeCooldown` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/components/NetworkDiagnosticsCard.kt` |
| `DeveloperMenuUiEvent` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiEvent.kt` |
| `DeveloperMenuUiState` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiState.kt` |
| `DeveloperNodesViewModel` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/nodes/DeveloperNodesViewModel.kt` |
| `DisclaimerViewModel` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/DisclaimerViewModel.kt` |
| `DisclaimerType` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/model/DisclaimerType.kt` |
| `DisclaimerUiEvent` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/model/DisclaimerUiEvent.kt` |
| `DeveloperErrorLogViewModel` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/DeveloperErrorLogViewModel.kt` |
| `DeveloperErrorLogUiEvent` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiEvent.kt` |
| `DeveloperErrorLogUiState` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiState.kt` |
| `DeveloperErrorUi` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorUi.kt` |
| `LicensesViewModel` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/LicensesViewModel.kt` |
| `LicensesUiEvent` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/model/LicensesUiEvent.kt` |
| `LicensesUiState` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/model/LicensesUiState.kt` |
| `ControlPlaneSettingsViewModel` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/ControlPlaneSettingsViewModel.kt` |
| `ControlPlaneAddSource` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneDirectoryError` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneSettingsError` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneSettingsUiEvent` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneSettingsUiState` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneUiModel` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneUiSource` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneUiStatus` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `SettingsViewModel` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/SettingsViewModel.kt` |
| `SettingsEffect` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsEffect.kt` |
| `SettingsUiEvent` | `interface` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `SettingsUiState` | `class` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiState.kt` |
