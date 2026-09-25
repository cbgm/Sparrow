# `:feature:autoreply`

Source directory: `feature/autoreply`

## Direct project dependencies

- `:core`
- `:core:ui`
- `:data:database`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AutoReplyDataSource` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/data/datasource/AutoReplyDataSource.kt` |
| `AutoReplyRepositoryImpl` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/data/repository/AutoReplyRepositoryImpl.kt` |
| `AutoReply` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/model/AutoReply.kt` |
| `AutoReplyRepository` | `interface` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/repository/AutoReplyRepository.kt` |
| `ActivateAutoReplyUseCase` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ActivateAutoReplyUseCase.kt` |
| `ClaimAutoReplyForContactUseCase` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ClaimAutoReplyForContactUseCase.kt` |
| `CreateAutoReplyUseCase` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/CreateAutoReplyUseCase.kt` |
| `DeactivateAutoReplyUseCase` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/DeactivateAutoReplyUseCase.kt` |
| `DeleteAutoReplyUseCase` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/DeleteAutoReplyUseCase.kt` |
| `ObserveActiveAutoReplyUseCase` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ObserveActiveAutoReplyUseCase.kt` |
| `ObserveAutoRepliesUseCase` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ObserveAutoRepliesUseCase.kt` |
| `ReleaseAutoReplyRecipientUseCase` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ReleaseAutoReplyRecipientUseCase.kt` |
| `UpdateAutoReplyUseCase` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/UpdateAutoReplyUseCase.kt` |
| `AutoReplyViewModel` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/AutoReplyViewModel.kt` |
| `AutoReplyEditorUiState` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiState.kt` |
| `AutoReplyEffect` | `interface` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `AutoReplyUiEvent` | `interface` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `AutoReplyUiItem` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiState.kt` |
| `AutoReplyUiState` | `class` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiState.kt` |
