# `:feature:safety`

Source directory: `feature/safety`

## Direct project dependencies

- `:core`
- `:core:embedding`
- `:core:ui`
- `:data:database`
- `:feature:contacts`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `MessageSafetyEmbeddingDataSource` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/datasource/MessageSafetyEmbeddingDataSource.kt` |
| `MessageSafetyLocalDataSource` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/datasource/MessageSafetyLocalDataSource.kt` |
| `GeneratedMessageSafetyMlpModel` | `object` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/model/GeneratedMessageSafetyMlpModel.kt` |
| `MessageSafetyAnalysisRepositoryImpl` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/repository/MessageSafetyAnalysisRepositoryImpl.kt` |
| `MessageSafetyRepositoryImpl` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/repository/MessageSafetyRepositoryImpl.kt` |
| `MessageSafetyAssessment` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyAssessment.kt` |
| `MessageSafetyCandidate` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyCandidate.kt` |
| `MessageSafetyReason` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyReason.kt` |
| `MessageSafetyState` | `interface` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyState.kt` |
| `MessageSafetyAnalysisRepository` | `interface` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/repository/MessageSafetyAnalysisRepository.kt` |
| `MessageSafetyRepository` | `interface` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/repository/MessageSafetyRepository.kt` |
| `AnalyzeMessageSafetyUseCase` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/AnalyzeMessageSafetyUseCase.kt` |
| `InitializeMessageSafetyUseCase` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/InitializeMessageSafetyUseCase.kt` |
| `ObserveMessageSafetyAssessmentsUseCase` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/ObserveMessageSafetyAssessmentsUseCase.kt` |
| `ObserveMessageSafetyStateUseCase` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/ObserveMessageSafetyStateUseCase.kt` |
| `ProcessMessageSafetyBatchUseCase` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/ProcessMessageSafetyBatchUseCase.kt` |
| `MessageSafetyDetailsViewModel` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/MessageSafetyDetailsViewModel.kt` |
| `MessageSafetyDetailsUiEvent` | `interface` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyDetailsUiEvent.kt` |
| `MessageSafetyDetailsUiState` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyDetailsUiState.kt` |
| `MessageSafetyWarningLevel` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyWarningLevel.kt` |
| `MessageSafetyWarningReason` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyWarningReason.kt` |
| `MessageSafetyWarningUi` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyWarningUi.kt` |
| `MessageSafetyClassifier` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/util/MessageSafetyClassifier.kt` |
| `MessageSafetyStructuralAnalyzer` | `class` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/util/MessageSafetyStructuralAnalyzer.kt` |
