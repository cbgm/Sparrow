# `:feature:invite`

Source directory: `feature/invite`

## Direct project dependencies

- `:core`
- `:core:ui`
- `:feature:avatar`
- `:data:database`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `InvitationLifecycleDataSource` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/data/datasource/InvitationLifecycleDataSource.kt` |
| `InvitationOutboxDeliveryHandler` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/data/outbox/InvitationOutboxDeliveryHandler.kt` |
| `InvitationRepositoryImpl` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/data/repository/InvitationRepositoryImpl.kt` |
| `Invitation` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationDirection` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationLifecycleRecord` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationLifecycleRecord.kt` |
| `InvitationLifecycleStatus` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationLifecycleStatus.kt` |
| `InvitationPayloadType` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationResponse` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationResponse.kt` |
| `InvitationResult` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationResult.kt` |
| `InvitationResultAction` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationResultAction.kt` |
| `InvitationStatus` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationsContext` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationRepository` | `interface` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/repository/InvitationRepository.kt` |
| `AcceptInvitationUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/AcceptInvitationUseCase.kt` |
| `DeclineAndBlockInvitationUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/DeclineAndBlockInvitationUseCase.kt` |
| `DeclineInvitationUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/DeclineInvitationUseCase.kt` |
| `DeleteDeclinedOutgoingInvitationUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/DeleteDeclinedOutgoingInvitationUseCase.kt` |
| `HandleInvitationResponseUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/HandleInvitationResponseUseCase.kt` |
| `InvalidatePendingInvitationUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/InvalidatePendingInvitationUseCase.kt` |
| `MarkInvitationTransportFailedUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/MarkInvitationTransportFailedUseCase.kt` |
| `MarkInvitationsViewedUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/MarkInvitationsViewedUseCase.kt` |
| `ObserveInvitationLifecycleStatusUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationLifecycleStatusUseCase.kt` |
| `ObserveInvitationResultsUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationResultsUseCase.kt` |
| `ObserveInvitationsContextUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationsContextUseCase.kt` |
| `ObserveInvitationsUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationsUseCase.kt` |
| `ObservePendingInvitationCountUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObservePendingInvitationCountUseCase.kt` |
| `ObservePendingInvitationsUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObservePendingInvitationsUseCase.kt` |
| `RecordPendingInvitationUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/RecordPendingInvitationUseCase.kt` |
| `ShouldRecordPendingInvitationUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ShouldRecordPendingInvitationUseCase.kt` |
| `ValidatePendingInvitationUseCase` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ValidatePendingInvitationUseCase.kt` |
| `InvitationStatusPresentation` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/InvitationsScreen.kt` |
| `InvitationViewModel` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/InvitationViewModel.kt` |
| `InvitationEffect` | `interface` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationEffect.kt` |
| `InvitationTab` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUi` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiDirection` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiEvent` | `interface` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiEvent.kt` |
| `InvitationUiPayloadType` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiState` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiStatus` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationsUiData` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `MailboxReviewRequestUi` | `class` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
