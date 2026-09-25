# `:startup`

Source directory: `startup`

## Direct project dependencies

- `:core`
- `:core:ui`
- `:core:embedding`
- `:feature:identity`
- `:feature:onboarding`
- `:feature:search`
- `:feature:safety`
- `:feature:transport`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AppConnectionAvailability` | `class` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/model/AppConnectionAvailability.kt` |
| `ObserveAppConnectionAvailabilityUseCase` | `class` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/usecase/ObserveAppConnectionAvailabilityUseCase.kt` |
| `StartupViewModel` | `class` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/StartupViewModel.kt` |
| `AppInitializationResult` | `interface` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/AppInitializationResult.kt` |
| `StartupConnection` | `class` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiState.kt` |
| `StartupUiEvent` | `interface` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiEvent.kt` |
| `StartupUiState` | `interface` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiState.kt` |
| `AppInitializer` | `class` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/util/AppInitializer.kt` |
| `StartupRuntimeReadiness` | `class` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/util/StartupRuntimeReadiness.kt` |
