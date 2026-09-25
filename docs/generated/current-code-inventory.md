# Current code inventory

This page is generated directly from the **current source tree** for documentation auditing. It lists production top-level Kotlin declarations by Gradle module so hand-written documentation can be checked against actual classes. Tests are intentionally excluded.

Gradle modules in `settings.gradle.kts`: **44**.

## `:androidApp`

Path: `androidApp`  
Direct project dependencies detected from `build.gradle.kts`: `:shared`

Production top-level declarations: **2**

| Class/type | Source set | File |
|---|---|---|
| `MainActivity` | `main` | `androidApp/src/main/kotlin/com/cbgm/sparrow/MainActivity.kt` |
| `SparrowApplication` | `main` | `androidApp/src/main/kotlin/com/cbgm/sparrow/SparrowApplication.kt` |

## `:shared`

Path: `shared`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:data:datastore`, `:core:embedding`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:navigation`, `:feature:autoreply`, `:feature:avatar`, `:feature:chats`, `:feature:attachments`, `:feature:contactimport`, `:feature:contacts`, `:feature:conversationorchestration`, `:feature:invite`, `:feature:identity`, `:feature:media`, `:feature:membership`, `:feature:voice`, `:feature:linkpreview`, `:feature:messaging`, `:feature:onboarding`, `:feature:settings`, `:feature:search`, `:feature:safety`, `:feature:transport`, `:notification`, `:startup`, `:data:database`

Production top-level declarations: **7**

| Class/type | Source set | File |
|---|---|---|
| `AndroidPlatform` | `androidMain` | `shared/src/androidMain/kotlin/com/cbgm/sparrow/device/Platform.android.kt` |
| `Platform` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/device/Platform.kt` |
| `AppViewModel` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/presentation/AppViewModel.kt` |
| `AppInitializationDependencies` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/presentation/model/AppInitializationDependencies.kt` |
| `ForegroundRuntimeDependencies` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/presentation/model/ForegroundRuntimeDependencies.kt` |
| `AttachmentConversationNameObserver` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/AttachmentConversationNameObserver.kt` |
| `IOSPlatform` | `iosMain` | `shared/src/iosMain/kotlin/com/cbgm/sparrow/device/Platform.ios.kt` |

## `:core`

Path: `core`  
Direct project dependencies detected from `build.gradle.kts`: none

Production top-level declarations: **20**

| Class/type | Source set | File |
|---|---|---|
| `ApplicationCoroutineScope` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/coroutines/ApplicationCoroutineScope.kt` |
| `IdGenerator` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/id/IdGenerator.kt` |
| `ChatOpenTrace` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/logging/ChatOpenTrace.kt` |
| `KermitSparrowLogger` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/logging/SparrowLogger.kt` |
| `SparrowLog` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/logging/SparrowLogger.kt` |
| `SparrowLogger` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/logging/SparrowLogger.kt` |
| `StartupTrace` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/logging/StartupTrace.kt` |
| `SystemClock` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/time/SystemClock.kt` |
| `ControlPlaneConfiguration` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneDirectorySynchronizer` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneEndpoint` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneEndpointStatus` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneHealthMonitor` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneReachability` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneStatusStore` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/ControlPlaneConfiguration.kt` |
| `TransportDiagnosticConnectionState` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/TransportDiagnostics.kt` |
| `TransportDiagnostics` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/TransportDiagnostics.kt` |
| `TransportDiagnosticsProvider` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/TransportDiagnostics.kt` |
| `TransportNodeDiagnostic` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/TransportDiagnostics.kt` |
| `TransportNodeDiagnosticState` | `commonMain` | `core/src/commonMain/kotlin/com/cbgm/sparrow/core/transport/TransportDiagnostics.kt` |

## `:data:datastore`

Path: `data/datastore`  
Direct project dependencies detected from `build.gradle.kts`: none

Production top-level declarations: **2**

| Class/type | Source set | File |
|---|---|---|
| `SparrowDataStore` | `commonMain` | `data/datastore/src/commonMain/kotlin/com/cbgm/sparrow/data/datastore/SparrowDataStore.kt` |
| `SparrowDataStoreEditor` | `commonMain` | `data/datastore/src/commonMain/kotlin/com/cbgm/sparrow/data/datastore/SparrowDataStore.kt` |

## `:core:embedding`

Path: `core/embedding`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:data:datastore`

Production top-level declarations: **18**

| Class/type | Source set | File |
|---|---|---|
| `AndroidLocalEmbeddingModelDownloader` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/AndroidLocalEmbeddingModelDownloader.kt` |
| `AndroidLocalEmbeddingModelFiles` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/AndroidLocalEmbeddingModelFiles.kt` |
| `AndroidLocalEmbeddingModelManager` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/AndroidLocalEmbeddingModelManager.kt` |
| `MediaPipeLocalTextEmbedder` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/MediaPipeLocalTextEmbedder.kt` |
| `LocalEmbeddingModelDownloadWorker` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/work/LocalEmbeddingModelDownloadWorker.kt` |
| `LocalEmbeddingModel` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/model/LocalEmbeddingModel.kt` |
| `EmbeddingInputType` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/LocalTextEmbedder.kt` |
| `LocalEmbeddingModelManager` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/LocalEmbeddingModelManager.kt` |
| `LocalTextEmbedder` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/LocalTextEmbedder.kt` |
| `LocalEmbeddingRepositoryImpl` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/repository/LocalEmbeddingRepositoryImpl.kt` |
| `LocalEmbeddingSettingsStorage` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/storage/LocalEmbeddingSettingsStorage.kt` |
| `LocalEmbeddingFeature` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/model/LocalEmbeddingFeature.kt` |
| `LocalEmbeddingModelState` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/model/LocalEmbeddingModelState.kt` |
| `LocalEmbeddingState` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/model/LocalEmbeddingState.kt` |
| `LocalEmbeddingRepository` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/repository/LocalEmbeddingRepository.kt` |
| `InitializeLocalEmbeddingUseCase` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/usecase/InitializeLocalEmbeddingUseCase.kt` |
| `ObserveLocalEmbeddingStateUseCase` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/usecase/ObserveLocalEmbeddingStateUseCase.kt` |
| `SetLocalEmbeddingFeatureEnabledUseCase` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/usecase/SetLocalEmbeddingFeatureEnabledUseCase.kt` |

## `:feature:identity`

Path: `feature/identity`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:data:datastore`, `:data:database`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:feature:avatar`

Production top-level declarations: **155**

| Class/type | Source set | File |
|---|---|---|
| `AndroidIdentityBackupCodec` | `androidMain` | `feature/identity/src/androidMain/kotlin/com/cbgm/sparrow/feature/identity/device/AndroidIdentityBackupCodec.kt` |
| `AndroidPrivateKeyStorage` | `androidMain` | `feature/identity/src/androidMain/kotlin/com/cbgm/sparrow/feature/identity/device/AndroidPrivateKeyStorage.kt` |
| `IdentityLocalResetHandler` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/IdentityLocalResetHandler.kt` |
| `ApprovedIdentityReconnectionDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/ApprovedIdentityReconnectionDataSource.kt` |
| `IdentityBackupStatusDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityBackupStatusDataSource.kt` |
| `IdentityExchangeDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityExchangeDataSource.kt` |
| `IdentityExchangeStoreDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityExchangeStoreDataSource.kt` |
| `IdentityVerificationDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityVerificationDataSource.kt` |
| `LocalIdentityProfileDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/LocalIdentityProfileDataSource.kt` |
| `LocalIdentitySharingDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/LocalIdentitySharingDataSource.kt` |
| `LocalProfilePictureDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/LocalProfilePictureDataSource.kt` |
| `ManualIdentityExchangeDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/ManualIdentityExchangeDataSource.kt` |
| `PendingRemoteIdentityChangeDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/PendingRemoteIdentityChangeDataSource.kt` |
| `ProfilePictureFileDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/ProfilePictureFileDataSource.kt` |
| `PublicIdentityDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/PublicIdentityDataSource.kt` |
| `RemoteIdentityDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/RemoteIdentityDataSource.kt` |
| `RemoteProfilePictureDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/RemoteProfilePictureDataSource.kt` |
| `SparrowDataStorePublicIdentityDataSource` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/SparrowDataStorePublicIdentityDataSource.kt` |
| `IdentityAcceptanceReviewRequiredDtoException` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/IdentityAcceptanceReviewRequiredDtoException.kt` |
| `IdentityExchangeBindingDto` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/IdentityExchangeBindingDto.kt` |
| `IdentityExchangeStage` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/IdentityExchangeStage.kt` |
| `RemoteIdentityImportOriginDto` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/RemoteIdentityImportOriginDto.kt` |
| `StoredKeyExchangeStatusDto` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/StoredRemoteIdentityStateDto.kt` |
| `StoredVerificationStatusDto` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/StoredRemoteIdentityStateDto.kt` |
| `IdentityVerificationReceiptEncoder` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/protocol/IdentityVerificationReceiptEncoder.kt` |
| `ApprovedIdentityReconnectionRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/ApprovedIdentityReconnectionRepositoryImpl.kt` |
| `IdentityBackupRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityBackupRepositoryImpl.kt` |
| `IdentityExchangeRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityExchangeRepositoryImpl.kt` |
| `IdentityRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityRepositoryImpl.kt` |
| `IdentityShareRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityShareRepositoryImpl.kt` |
| `IdentityVerificationRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityVerificationRepositoryImpl.kt` |
| `LocalIdentityProfileRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/LocalIdentityProfileRepositoryImpl.kt` |
| `LocalIdentitySharingRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/LocalIdentitySharingRepositoryImpl.kt` |
| `LocalProfilePictureRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/LocalProfilePictureRepositoryImpl.kt` |
| `PendingRemoteIdentityChangeRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/PendingRemoteIdentityChangeRepositoryImpl.kt` |
| `RemoteIdentityImportRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteIdentityImportRepositoryImpl.kt` |
| `RemoteIdentityReadRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteIdentityReadRepositoryImpl.kt` |
| `RemoteProfilePictureRepositoryImpl` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteProfilePictureRepositoryImpl.kt` |
| `IdentityBackupCodec` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/IdentityBackupCodec.kt` |
| `IdentityExportRequest` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/IdentityBackupDocumentLauncher.kt` |
| `PhoneNumberHintResult` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PhoneNumberHintLauncher.kt` |
| `PrivateKeyStorage` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PrivateKeyStorage.kt` |
| `IdentityAcceptanceRequiresReviewException` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/error/IdentityAcceptanceRequiresReviewException.kt` |
| `ApprovedIdentityReconnection` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/ApprovedIdentityReconnection.kt` |
| `ContactVerificationStatus` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/ContactVerificationStatus.kt` |
| `DirectIdentitySetupMode` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/DirectIdentitySetupMode.kt` |
| `IdentityBackup` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityBackup.kt` |
| `IdentityBackupStatus` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityBackupStatus.kt` |
| `IdentityExchange` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchange.kt` |
| `IdentityExchangeAcceptance` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInput.kt` |
| `IdentityExchangeBinding` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchangeBinding.kt` |
| `IdentityExchangeClosure` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchangeClosure.kt` |
| `IdentityExchangeClosurePhase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchangeClosure.kt` |
| `IdentityExchangeDirection` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchange.kt` |
| `IdentityExchangeOffer` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInput.kt` |
| `IdentityExchangeReady` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInput.kt` |
| `IdentityHandshakeState` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeState.kt` |
| `IdentityPeerState` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityPeerState.kt` |
| `IdentityResult` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityResult.kt` |
| `IdentityResultStatus` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityResult.kt` |
| `IdentityStatus` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityStatus.kt` |
| `KeyExchangeStatus` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/KeyExchangeStatus.kt` |
| `LocalProfilePicture` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/LocalProfilePicture.kt` |
| `PendingRemoteIdentityChange` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PendingRemoteIdentityChange.kt` |
| `PublicIdentity` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PublicIdentity.kt` |
| `RemoteIdentityOrigin` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemoteIdentityOrigin.kt` |
| `RemoteIdentityUpdate` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemoteIdentityUpdate.kt` |
| `RemotePeerIdentity` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemotePeerIdentity.kt` |
| `RemoteProfilePicture` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemoteProfilePicture.kt` |
| `SharedContactDetails` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/SharedIdentityPayload.kt` |
| `SharedIdentityPayload` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/SharedIdentityPayload.kt` |
| `ApprovedIdentityReconnectionRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/ApprovedIdentityReconnectionRepository.kt` |
| `DirectIdentitySetupModeRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/DirectIdentitySetupModeRepository.kt` |
| `IdentityBackupRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityBackupRepository.kt` |
| `IdentityExchangeRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityExchangeRepository.kt` |
| `IdentityRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityRepository.kt` |
| `IdentityShareRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityShareRepository.kt` |
| `IdentityVerificationRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityVerificationRepository.kt` |
| `LocalIdentityProfileRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/LocalIdentityProfileRepository.kt` |
| `LocalIdentitySharingRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/LocalIdentitySharingRepository.kt` |
| `LocalProfilePictureRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/LocalProfilePictureRepository.kt` |
| `PendingRemoteIdentityChangeRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/PendingRemoteIdentityChangeRepository.kt` |
| `RemoteIdentityImportRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/RemoteIdentityImportRepository.kt` |
| `RemoteIdentityReadRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/RemoteIdentityReadRepository.kt` |
| `RemoteProfilePictureRepository` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/RemoteProfilePictureRepository.kt` |
| `AcceptIdentityExchangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/AcceptIdentityExchangeUseCase.kt` |
| `AcceptRemoteIdentityHandshakeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/AcceptRemoteIdentityHandshakeUseCase.kt` |
| `AcknowledgeQueuedRecoveryInvitationUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/AcknowledgeQueuedRecoveryInvitationUseCase.kt` |
| `ApplyRemoteProfilePictureMetadataUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ApplyRemoteProfilePictureMetadataUseCase.kt` |
| `ApprovePendingRemoteIdentityChangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ApprovePendingRemoteIdentityChangeUseCase.kt` |
| `CancelIdentityExchangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CancelIdentityExchangeUseCase.kt` |
| `CloseIdentityExchangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CloseIdentityExchangeUseCase.kt` |
| `CreateIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CreateIdentityUseCase.kt` |
| `CreateSharedIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CreateSharedIdentityUseCase.kt` |
| `DeclineIdentityExchangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DeclineIdentityExchangeUseCase.kt` |
| `DeclinePendingRemoteIdentityChangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DeclinePendingRemoteIdentityChangeUseCase.kt` |
| `DecodeSharedIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DecodeSharedIdentityUseCase.kt` |
| `DismissPendingRemoteIdentityChangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DismissPendingRemoteIdentityChangeUseCase.kt` |
| `EnsureRemoteSigningIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/EnsureRemoteSigningIdentityUseCase.kt` |
| `EstablishMutualIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/EstablishMutualIdentityUseCase.kt` |
| `FindRemoteIdentityPeerIdUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/FindRemoteIdentityPeerIdUseCase.kt` |
| `GetApprovedIdentityReconnectionUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetApprovedIdentityReconnectionUseCase.kt` |
| `GetIdentityBackupStatusUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `GetIdentityExchangeBindingUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityExchangeBindingUseCase.kt` |
| `GetIdentityExchangeClosureUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityExchangeClosureUseCase.kt` |
| `GetIdentityPeerStateUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityPeerStateUseCase.kt` |
| `GetIdentityStatusUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityStatusUseCase.kt` |
| `GetLocalPhoneNumberUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetLocalPhoneNumberUseCase.kt` |
| `GetPublicIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetPublicIdentityUseCase.kt` |
| `GetRemoteIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetRemoteIdentityUseCase.kt` |
| `HandleIdentityVerificationReceiptUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/HandleIdentityVerificationReceiptUseCase.kt` |
| `ImportRemoteIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ImportRemoteIdentityUseCase.kt` |
| `InvalidateIdentityExchangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/InvalidateIdentityExchangeUseCase.kt` |
| `MarkIdentityBackupExportedUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `NormalizeLocalPhoneNumberUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/NormalizeLocalPhoneNumberUseCase.kt` |
| `ObserveApprovedIdentityReconnectionsUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveApprovedIdentityReconnectionsUseCase.kt` |
| `ObserveIdentityHandshakeStateUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveIdentityHandshakeStateUseCase.kt` |
| `ObserveIdentityResultsUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveIdentityResultsUseCase.kt` |
| `ObserveLocalIdentityReadyUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveLocalIdentityReadyUseCase.kt` |
| `ObserveLocalIdentitySharedUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveLocalIdentitySharedUseCase.kt` |
| `ObserveLocalProfilePictureUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveLocalProfilePictureUseCase.kt` |
| `ObservePendingRemoteIdentityChangesUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObservePendingRemoteIdentityChangesUseCase.kt` |
| `ObserveRemoteIdentitiesUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveRemoteIdentitiesUseCase.kt` |
| `PrepareIdentityBackupUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `ReassignIdentityExchangePeerUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReassignIdentityExchangePeerUseCase.kt` |
| `ReceiveIdentityAcknowledgementUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityAcknowledgementUseCase.kt` |
| `ReceiveIdentityExchangeAcceptedUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityExchangeAcceptedUseCase.kt` |
| `ReceiveIdentityExchangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityExchangeUseCase.kt` |
| `ReceiveIdentityReadyUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityReadyUseCase.kt` |
| `ReceiveManualIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveManualIdentityUseCase.kt` |
| `RecordLocalIdentitySharedUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecordLocalIdentitySharedUseCase.kt` |
| `RecordRemoteIdentityDeclineUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecordRemoteIdentityDeclineUseCase.kt` |
| `RecoverIncompleteIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecoverIncompleteIdentityUseCase.kt` |
| `RecoverManualIdentityExchangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecoverManualIdentityExchangeUseCase.kt` |
| `RemoveLocalProfilePictureUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RemoveLocalProfilePictureUseCase.kt` |
| `RestoreIdentityBackupUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `SaveLocalPhoneNameUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SaveLocalPhoneNameUseCase.kt` |
| `SendIdentityVerificationReceiptUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SendIdentityVerificationReceiptUseCase.kt` |
| `SetLocalProfilePictureUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SetLocalProfilePictureUseCase.kt` |
| `StagePendingRemoteIdentityChangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StagePendingRemoteIdentityChangeUseCase.kt` |
| `StageRemoteIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StageRemoteIdentityUseCase.kt` |
| `StartIdentityExchangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StartIdentityExchangeUseCase.kt` |
| `StartManualIdentityExchangeUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StartManualIdentityExchangeUseCase.kt` |
| `VerifyRemoteIdentityUseCase` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/VerifyRemoteIdentityUseCase.kt` |
| `IdentityViewModel` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/IdentityViewModel.kt` |
| `MeDetailPage` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/IdentityScreen.kt` |
| `IdentityBackupUiState` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/model/IdentityBackupUiState.kt` |
| `IdentityBackupUiStatus` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/model/IdentityBackupUiState.kt` |
| `IdentityUiEvent` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/model/IdentityUiEvent.kt` |
| `IdentityUiState` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/model/IdentityUiState.kt` |
| `IdentityProfilePictureUiState` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/profile/IdentityProfilePictureUiState.kt` |
| `IdentityProfilePictureViewModel` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/profile/IdentityProfilePictureViewModel.kt` |
| `ShareIdentityViewModel` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/ShareIdentityViewModel.kt` |
| `ShareIdentityUiEvent` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/model/ShareIdentityUiEvent.kt` |
| `ShareIdentityUiState` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/model/ShareIdentityUiState.kt` |

## `:feature:invite`

Path: `feature/invite`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:ui`, `:feature:avatar`, `:data:database`

Production top-level declarations: **43**

| Class/type | Source set | File |
|---|---|---|
| `InvitationLifecycleDataSource` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/data/datasource/InvitationLifecycleDataSource.kt` |
| `InvitationOutboxDeliveryHandler` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/data/outbox/InvitationOutboxDeliveryHandler.kt` |
| `InvitationRepositoryImpl` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/data/repository/InvitationRepositoryImpl.kt` |
| `Invitation` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationDirection` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationLifecycleRecord` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationLifecycleRecord.kt` |
| `InvitationLifecycleStatus` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationLifecycleStatus.kt` |
| `InvitationPayloadType` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationResponse` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationResponse.kt` |
| `InvitationResult` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationResult.kt` |
| `InvitationResultAction` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationResultAction.kt` |
| `InvitationStatus` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationsContext` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationRepository` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/repository/InvitationRepository.kt` |
| `AcceptInvitationUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/AcceptInvitationUseCase.kt` |
| `DeclineAndBlockInvitationUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/DeclineAndBlockInvitationUseCase.kt` |
| `DeclineInvitationUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/DeclineInvitationUseCase.kt` |
| `DeleteDeclinedOutgoingInvitationUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/DeleteDeclinedOutgoingInvitationUseCase.kt` |
| `HandleInvitationResponseUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/HandleInvitationResponseUseCase.kt` |
| `InvalidatePendingInvitationUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/InvalidatePendingInvitationUseCase.kt` |
| `MarkInvitationTransportFailedUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/MarkInvitationTransportFailedUseCase.kt` |
| `MarkInvitationsViewedUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/MarkInvitationsViewedUseCase.kt` |
| `ObserveInvitationLifecycleStatusUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationLifecycleStatusUseCase.kt` |
| `ObserveInvitationResultsUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationResultsUseCase.kt` |
| `ObserveInvitationsContextUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationsContextUseCase.kt` |
| `ObserveInvitationsUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationsUseCase.kt` |
| `ObservePendingInvitationCountUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObservePendingInvitationCountUseCase.kt` |
| `ObservePendingInvitationsUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObservePendingInvitationsUseCase.kt` |
| `RecordPendingInvitationUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/RecordPendingInvitationUseCase.kt` |
| `ShouldRecordPendingInvitationUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ShouldRecordPendingInvitationUseCase.kt` |
| `ValidatePendingInvitationUseCase` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ValidatePendingInvitationUseCase.kt` |
| `InvitationStatusPresentation` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/InvitationsScreen.kt` |
| `InvitationViewModel` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/InvitationViewModel.kt` |
| `InvitationEffect` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationEffect.kt` |
| `InvitationTab` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUi` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiDirection` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiEvent` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiEvent.kt` |
| `InvitationUiPayloadType` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiState` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiStatus` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationsUiData` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `MailboxReviewRequestUi` | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |

## `:feature:contacts`

Path: `feature/contacts`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:feature:avatar`, `:feature:identity`, `:feature:transport`, `:data:database`

Production top-level declarations: **88**

| Class/type | Source set | File |
|---|---|---|
| `AndroidDeviceContactWriterRepository` | `androidMain` | `feature/contacts/src/androidMain/kotlin/com/cbgm/sparrow/feature/contacts/device/AndroidDeviceContactWriterRepository.kt` |
| `AndroidDeviceContactsRepository` | `androidMain` | `feature/contacts/src/androidMain/kotlin/com/cbgm/sparrow/feature/contacts/device/AndroidDeviceContactsRepository.kt` |
| `ContactByRoutingIdDataSource` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactByRoutingIdDataSource.kt` |
| `ContactLocalDataSource` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactLocalDataSource.kt` |
| `ContactRoutingDataSource` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactRoutingDataSource.kt` |
| `ContactRoutingIdDataSource` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactRoutingIdDataSource.kt` |
| `ContactRoutingReconciliationDataSource` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactRoutingReconciliationDataSource.kt` |
| `MailboxContactDataSource` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/MailboxContactDataSource.kt` |
| `MailboxContactStateDto` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/MailboxContactDataSource.kt` |
| `ContactRepositoryImpl` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/ContactRepositoryImpl.kt` |
| `ContactTransportRepositoryImpl` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/ContactTransportRepositoryImpl.kt` |
| `IdentityPeerRepositoryImpl` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/IdentityPeerRepositoryImpl.kt` |
| `BlockedContactsContext` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/BlockedContactsContext.kt` |
| `Contact` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/Contact.kt` |
| `ContactBlocklist` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactBlocklist.kt` |
| `ContactDetailsContext` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactDetailsContext.kt` |
| `ContactPhoneNumber` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactPhoneNumber.kt` |
| `ContactPhoneNumberType` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactPhoneNumberType.kt` |
| `ContactsWithProfilePictures` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactsWithProfilePictures.kt` |
| `DeviceContactLinkStatus` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/DeviceContactLinkStatus.kt` |
| `IdentityImportTrust` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/IdentityImportTrust.kt` |
| `ImportContactRequest` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ImportContactRequest.kt` |
| `ImportDeviceContactRequest` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ImportDeviceContactRequest.kt` |
| `ImportDevicePhoneNumber` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ImportDevicePhoneNumber.kt` |
| `IncomingPeerContactCandidate` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/IncomingPeerContactCandidate.kt` |
| `MailboxContactState` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/MailboxContactState.kt` |
| `SparrowIdentity` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/SparrowIdentity.kt` |
| `AddDeviceContactRequest` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `AddDeviceContactResult` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `DeviceContact` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/DeviceContact.kt` |
| `DevicePhoneNumber` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/DevicePhoneNumber.kt` |
| `DevicePhoneNumberType` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/DevicePhoneNumberType.kt` |
| `IdentityPeerMerge` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/identity/IdentityPeerResolution.kt` |
| `IdentityPeerResolution` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/identity/IdentityPeerResolution.kt` |
| `ContactBlocklistRepository` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/ContactBlocklistRepository.kt` |
| `ContactRepository` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/ContactRepository.kt` |
| `ContactTransportRepository` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/ContactTransportRepository.kt` |
| `DeviceContactWriterRepository` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/DeviceContactWriterRepository.kt` |
| `DeviceContactsRepository` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/DeviceContactsRepository.kt` |
| `IdentityPeerRepository` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/IdentityPeerRepository.kt` |
| `AddDeviceContactUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/AddDeviceContactUseCase.kt` |
| `BlockContactUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/BlockContactUseCase.kt` |
| `GetContactSafetyNumberUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetContactSafetyNumberUseCase.kt` |
| `GetContactUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetContactUseCase.kt` |
| `GetMailboxContactStatesUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetMailboxContactStatesUseCase.kt` |
| `GetMutualContactSigningPublicKeyUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetMutualContactSigningPublicKeyUseCase.kt` |
| `ImportContactUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ImportContactUseCase.kt` |
| `ImportDeviceContactsUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ImportDeviceContactsUseCase.kt` |
| `ObserveBlockedContactsContextUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveBlockedContactsContextUseCase.kt` |
| `ObserveContactBlocklistUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactBlocklistUseCase.kt` |
| `ObserveContactDetailsContextUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactDetailsContextUseCase.kt` |
| `ObserveContactUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactUseCase.kt` |
| `ObserveContactsUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactsUseCase.kt` |
| `ObserveIdentitySetupModeUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveIdentitySetupModeUseCase.kt` |
| `ReconcileContactTransportRoutingUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ReconcileContactTransportRoutingUseCase.kt` |
| `ResolveContactBootstrapRoutingIdUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactBootstrapRoutingIdUseCase.kt` |
| `ResolveContactIdByRoutingIdUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactIdByRoutingIdUseCase.kt` |
| `ResolveContactInvitationRoutingIdUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactInvitationRoutingIdUseCase.kt` |
| `ResolveContactTransportRoutingIdUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactTransportRoutingIdUseCase.kt` |
| `ResolveIncomingPeerContactsUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveIncomingPeerContactsUseCase.kt` |
| `UnblockContactUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/UnblockContactUseCase.kt` |
| `ApplyIdentityPeerMergeUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/ApplyIdentityPeerMergeUseCase.kt` |
| `GetIdentityPeerDisplayNameUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/GetIdentityPeerDisplayNameUseCase.kt` |
| `InspectContactPeerUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/InspectContactPeerUseCase.kt` |
| `UpdateIncomingIdentityPeerMetadataUseCase` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/UpdateIncomingIdentityPeerMetadataUseCase.kt` |
| `BlockedContactsViewModel` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/BlockedContactsViewModel.kt` |
| `BlockedContactsEffect` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsEffect.kt` |
| `BlockedContactsUiEvent` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `BlockedContactsUiState` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiState.kt` |
| `ContactDetailsContent` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/ContactDetailsRoute.kt` |
| `ContactDetailsViewModel` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/ContactDetailsViewModel.kt` |
| `ContactDetailPage` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/components/ContactDetailsContent.kt` |
| `ContactDetailsPreviewData` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/components/ContactDetailsPreviewData.kt` |
| `ContactDetailsContactUi` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `ContactDetailsUiEvent` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `ContactDetailsUiState` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiState.kt` |
| `ContactPhoneNumberTypeUi` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `ContactPhoneNumberUi` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `DeviceContactLinkUi` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `SparrowIdentityUi` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `ContactsListMode` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/ContactsScreen.kt` |
| `ContactsViewModel` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/ContactsViewModel.kt` |
| `ContactGroupEntity` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactGroupEntity.kt` |
| `ContactUi` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactUi.kt` |
| `ContactsEffect` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsEffect.kt` |
| `ContactsScreenMode` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsScreenMode.kt` |
| `ContactsUiEvent` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `ContactsUiState` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiState.kt` |

## `:data:database`

Path: `data/database`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:protocol`

Production top-level declarations: **80**

| Class/type | Source set | File |
|---|---|---|
| `ApprovedIdentityReconnectionDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ApprovedIdentityReconnectionDao.kt` |
| `AutoReplyDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/AutoReplyDao.kt` |
| `ChatDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ChatDao.kt` |
| `ContactDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ContactDao.kt` |
| `ContactRoutingIdDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ContactRoutingIdDao.kt` |
| `GroupMembershipDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupMembershipDao.kt` |
| `GroupPinDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupPinDao.kt` |
| `GroupSecurityDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupSecurityDao.kt` |
| `GroupVerificationDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupVerificationDao.kt` |
| `IdentityExchangeDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/IdentityExchangeDao.kt` |
| `InvitationDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/InvitationDao.kt` |
| `LinkPreviewDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/LinkPreviewDao.kt` |
| `MailboxRouteDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MailboxRouteDao.kt` |
| `MessageAttachmentDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageAttachmentDao.kt` |
| `MessageDeliveryStatusDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageDeliveryStatusDao.kt` |
| `MessageReactionDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageReactionDao.kt` |
| `MessageRecipientStateDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageRecipientStateDao.kt` |
| `MessageSafetyDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageSafetyDao.kt` |
| `MessageSearchDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageSearchDao.kt` |
| `PendingRemoteIdentityChangeDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/PendingRemoteIdentityChangeDao.kt` |
| `ProtocolOutboxDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ProtocolOutboxDao.kt` |
| `RemoteIdentityDao` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/RemoteIdentityDao.kt` |
| `ApprovedIdentityReconnectionEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ApprovedIdentityReconnectionEntity.kt` |
| `AttachmentMessageContextEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/AttachmentMessageContextEntity.kt` |
| `AutoReplyEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/AutoReplyEntity.kt` |
| `AutoReplyRecipientEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/AutoReplyRecipientEntity.kt` |
| `ContactEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactEntity.kt` |
| `ContactPhoneNumberEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactPhoneNumberEntity.kt` |
| `ContactPublicIdentityEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactPublicIdentityEntity.kt` |
| `ContactRoutingIdEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactRoutingIdEntity.kt` |
| `ConversationEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationEntity.kt` |
| `ConversationParticipantEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationParticipantEntity.kt` |
| `ConversationParticipantRole` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationParticipantRole.kt` |
| `ConversationType` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationType.kt` |
| `GroupMemberKeyEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupMemberKeyEntity.kt` |
| `GroupMembershipEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupMembershipEntity.kt` |
| `GroupPinEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupPinEntity.kt` |
| `GroupSecurityStateEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupSecurityStateEntity.kt` |
| `GroupVerificationPairEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupVerificationPairEntity.kt` |
| `IdentityExchangeEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/IdentityExchangeEntity.kt` |
| `InvitationEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/InvitationEntity.kt` |
| `LinkPreviewEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/LinkPreviewEntity.kt` |
| `LocalMailboxCredentialEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MailboxRouteEntities.kt` |
| `MessageAttachmentEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageAttachmentEntity.kt` |
| `MessageEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageEntity.kt` |
| `MessageReactionEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageReactionEntity.kt` |
| `MessageRecipientStateEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageRecipientStateEntity.kt` |
| `MessageSafetyAssessmentEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageSafetyAssessmentEntity.kt` |
| `MessageSearchEmbeddingEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageSearchEmbeddingEntity.kt` |
| `PendingRemoteIdentityChangeEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/PendingRemoteIdentityChangeEntity.kt` |
| `ProtocolOutboxEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ProtocolOutboxEntity.kt` |
| `ProtocolOutboxFailureEventEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ProtocolOutboxFailureEventEntity.kt` |
| `RemoteMailboxRouteEntity` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MailboxRouteEntities.kt` |
| `LocalIdentityDataResetter` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/identity/LocalIdentityDataResetter.kt` |
| `RoomLocalIdentityDataResetter` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/identity/LocalIdentityDataResetter.kt` |
| `RoomMailboxRouteRepository` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/mailbox/RoomMailboxRouteRepository.kt` |
| `ApprovedIdentityOfferMigration52To53` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/ApprovedIdentityOfferMigration52To53.kt` |
| `ApprovedIdentityReconnectionMigration51To52` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/ApprovedIdentityReconnectionMigration51To52.kt` |
| `AttachmentMessageContextMigration45To46` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/AttachmentMessageContextMigration45To46.kt` |
| `AttachmentMessageContextMigration46To47` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/AttachmentMessageContextMigration46To47.kt` |
| `GroupMemberPhoneMigration47To48` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/GroupMemberPhoneMigration47To48.kt` |
| `IdentityExchangeMigration41To42` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/IdentityExchangeMigration41To42.kt` |
| `IdentityExchangeMigration43To44` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/IdentityExchangeMigration43To44.kt` |
| `InvitationPeerDetailsMigration44To45` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/InvitationPeerDetailsMigration44To45.kt` |
| `PendingRemoteIdentityChangeMigration49To50` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/PendingRemoteIdentityChangeMigration49To50.kt` |
| `PendingRemoteIdentityChangeMigration50To51` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/PendingRemoteIdentityChangeMigration50To51.kt` |
| `ProtocolOutboxFailuresMigration48To49` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/ProtocolOutboxFailuresMigration48To49.kt` |
| `ContactWithPhoneNumbersDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ContactWithPhoneNumbersDto.kt` |
| `ContactWithPublicIdentityDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ContactWithPublicIdentityDto.kt` |
| `ConversationSummaryDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ConversationSummaryDto.kt` |
| `ConversationWithMessagesDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ConversationWithMessagesDto.kt` |
| `LocalMessageAttachmentRowDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/LocalMessageAttachmentRowDto.kt` |
| `MessageCursorDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/MessageCursorDto.kt` |
| `MessageSafetySourceDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/MessageSafetySourceDto.kt` |
| `MessageSearchSourceDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/MessageSearchSourceDto.kt` |
| `StoredMessageEmbeddingDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/StoredMessageEmbeddingDto.kt` |
| `StoredMessageSearchMatchDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/StoredMessageSearchMatchDto.kt` |
| `UnreadIncomingMessageDto` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/UnreadIncomingMessageDto.kt` |
| `DefaultProtocolOutbox` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/outbox/DefaultProtocolOutbox.kt` |
| `DatabaseConstants` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/util/DatabaseConstants.kt` |

## `:feature:contactimport`

Path: `feature/contactimport`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:ui`, `:feature:contacts`, `:feature:identity`, `:feature:invite`, `:data:database`, `:core:protocol`

Production top-level declarations: **13**

| Class/type | Source set | File |
|---|---|---|
| `RotatedLuminance` | `androidMain` | `feature/contactimport/src/androidMain/kotlin/com/cbgm/sparrow/feature/contactimport/device/QrScanner.android.kt` |
| `ImportSharedIdentityUseCase` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/domain/usecase/ImportSharedIdentityUseCase.kt` |
| `VerifyContactByQrUseCase` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/domain/usecase/VerifyContactByQrUseCase.kt` |
| `ImportIdentityViewModel` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/ImportIdentityViewModel.kt` |
| `IdentityImportTrustUi` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/IdentityImportTrustUi.kt` |
| `ImportIdentityUiEvent` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiEvent.kt` |
| `ImportIdentityUiState` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiState.kt` |
| `ScanIdentityNavigationViewModel` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/ScanIdentityNavigationViewModel.kt` |
| `ScanIdentityUiEvent` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/model/ScanIdentityUiEvent.kt` |
| `ScannedIdentityPreview` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/model/ScannedIdentityPreview.kt` |
| `VerifyContactQrViewModel` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/VerifyContactQrViewModel.kt` |
| `VerifyContactQrUiEvent` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiEvent.kt` |
| `VerifyContactQrUiState` | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiState.kt` |

## `:feature:autoreply`

Path: `feature/autoreply`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:ui`, `:data:database`

Production top-level declarations: **19**

| Class/type | Source set | File |
|---|---|---|
| `AutoReplyDataSource` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/data/datasource/AutoReplyDataSource.kt` |
| `AutoReplyRepositoryImpl` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/data/repository/AutoReplyRepositoryImpl.kt` |
| `AutoReply` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/model/AutoReply.kt` |
| `AutoReplyRepository` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/repository/AutoReplyRepository.kt` |
| `ActivateAutoReplyUseCase` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ActivateAutoReplyUseCase.kt` |
| `ClaimAutoReplyForContactUseCase` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ClaimAutoReplyForContactUseCase.kt` |
| `CreateAutoReplyUseCase` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/CreateAutoReplyUseCase.kt` |
| `DeactivateAutoReplyUseCase` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/DeactivateAutoReplyUseCase.kt` |
| `DeleteAutoReplyUseCase` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/DeleteAutoReplyUseCase.kt` |
| `ObserveActiveAutoReplyUseCase` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ObserveActiveAutoReplyUseCase.kt` |
| `ObserveAutoRepliesUseCase` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ObserveAutoRepliesUseCase.kt` |
| `ReleaseAutoReplyRecipientUseCase` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ReleaseAutoReplyRecipientUseCase.kt` |
| `UpdateAutoReplyUseCase` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/UpdateAutoReplyUseCase.kt` |
| `AutoReplyViewModel` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/AutoReplyViewModel.kt` |
| `AutoReplyEditorUiState` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiState.kt` |
| `AutoReplyEffect` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `AutoReplyUiEvent` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `AutoReplyUiItem` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiState.kt` |
| `AutoReplyUiState` | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiState.kt` |

## `:feature:avatar`

Path: `feature/avatar`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:protocol`, `:core:ui`, `:feature:media`

Production top-level declarations: **28**

| Class/type | Source set | File |
|---|---|---|
| `AvatarDataSource` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/AvatarDataSource.kt` |
| `CachedAvatarImage` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/LocalAvatarImageDataSource.kt` |
| `LocalAvatarEditorDataSource` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/LocalAvatarEditorDataSource.kt` |
| `LocalAvatarImageDataSource` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/LocalAvatarImageDataSource.kt` |
| `AvatarEditorRepositoryImpl` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/repository/AvatarEditorRepositoryImpl.kt` |
| `AvatarRepositoryImpl` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/repository/AvatarRepositoryImpl.kt` |
| `ImagePickerLauncher` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/device/ImagePicker.kt` |
| `Avatar` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/Avatar.kt` |
| `AvatarEditResult` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarEditResult.kt` |
| `AvatarEditorImage` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarEditorImage.kt` |
| `AvatarImage` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarImage.kt` |
| `AvatarTarget` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarTarget.kt` |
| `ProfilePictureCropRegion` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/ProfilePictureCropRegion.kt` |
| `ProfilePictureSourceRect` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/ProfilePictureCropRegion.kt` |
| `AvatarEditorRepository` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/repository/AvatarEditorRepository.kt` |
| `AvatarRepository` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/repository/AvatarRepository.kt` |
| `ClearAvatarEditorUseCase` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/ClearAvatarEditorUseCase.kt` |
| `ConsumeAvatarEditResultUseCase` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/ConsumeAvatarEditResultUseCase.kt` |
| `CropAvatarEditorSourceUseCase` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/CropAvatarEditorSourceUseCase.kt` |
| `ObserveAvatarUseCase` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/ObserveAvatarUseCase.kt` |
| `PrepareAvatarEditorSourceUseCase` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/PrepareAvatarEditorSourceUseCase.kt` |
| `AvatarViewModel` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/AvatarViewModel.kt` |
| `AvatarEditorStrings` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/AvatarEditor.kt` |
| `AvatarEditorViewModel` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/AvatarEditorViewModel.kt` |
| `ProfilePictureCropGeometry` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/crop/ProfilePictureCropGeometry.kt` |
| `AvatarEditorUiState` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/model/AvatarEditorUiState.kt` |
| `AvatarUiState` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/model/AvatarUiState.kt` |
| `ImagePickerDelegate` | `iosMain` | `feature/avatar/src/iosMain/kotlin/com/cbgm/sparrow/feature/avatar/device/ImagePicker.ios.kt` |

## `:feature:linkpreview`

Path: `feature/linkpreview`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:ui`, `:data:database`

Production top-level declarations: **14**

| Class/type | Source set | File |
|---|---|---|
| `LocalLinkPreviewDataSource` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/datasource/LocalLinkPreviewDataSource.kt` |
| `RemoteLinkPreviewDataSource` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/datasource/RemoteLinkPreviewDataSource.kt` |
| `LinkPreviewDto` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/model/LinkPreviewDto.kt` |
| `LinkPreviewRequestDto` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/model/LinkPreviewDto.kt` |
| `LinkPreviewUnavailableException` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/model/LinkPreviewException.kt` |
| `LinkPreviewRepositoryImpl` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/repository/LinkPreviewRepositoryImpl.kt` |
| `LinkPreview` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/domain/model/LinkPreview.kt` |
| `LinkPreviewRepository` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/domain/repository/LinkPreviewRepository.kt` |
| `GetLinkPreviewUseCase` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/domain/usecase/GetLinkPreviewUseCase.kt` |
| `PrefetchLinkPreviewsUseCase` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/domain/usecase/PrefetchLinkPreviewsUseCase.kt` |
| `LinkPreviewViewModel` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/LinkPreviewViewModel.kt` |
| `LinkPreviewUi` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/LinkPreviewUi.kt` |
| `LinkPreviewUiState` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/LinkPreviewUiState.kt` |
| `TextContentPart` | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/TextContentPart.kt` |

## `:feature:chats`

Path: `feature/chats`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:data:datastore`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:feature:avatar`, `:data:database`, `:feature:autoreply`, `:feature:contactimport`, `:feature:contacts`, `:feature:conversationorchestration`, `:feature:attachments`, `:feature:identity`, `:feature:media`, `:feature:membership`, `:feature:voice`, `:feature:linkpreview`, `:feature:safety`, `:feature:transport`

Production top-level declarations: **283**

| Class/type | Source set | File |
|---|---|---|
| `IncomingMessageDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/datasource/IncomingMessageDataSource.kt` |
| `MessageHistoryDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/datasource/MessageHistoryDataSource.kt` |
| `MessageReactionDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/datasource/MessageReactionDataSource.kt` |
| `UnreadableTransportMessageDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/datasource/UnreadableTransportMessageDataSource.kt` |
| `DirectConversationDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/datasource/DirectConversationDataSource.kt` |
| `DirectDeliveryDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/datasource/DirectDeliveryDataSource.kt` |
| `DirectMessageDeliveryCoordinator` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/delivery/DirectMessageDeliveryCoordinator.kt` |
| `DirectOutboxDeliveryHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/delivery/DirectOutboxDeliveryHandler.kt` |
| `DirectIncomingPacketProcessor` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/incoming/DirectIncomingPacketProcessor.kt` |
| `DirectMessageDeletionPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/incoming/handler/DirectMessageDeletionPacketHandler.kt` |
| `DirectMessageEditPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/incoming/handler/DirectMessageEditPacketHandler.kt` |
| `DirectMessagePacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/incoming/handler/DirectMessagePacketHandler.kt` |
| `DirectReceiptPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/incoming/handler/DirectReceiptPacketHandler.kt` |
| `DirectAutomaticOutgoingGate` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/outgoing/DirectAutomaticOutgoingGate.kt` |
| `DirectOutgoingMessageProcessor` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/outgoing/DirectOutgoingMessageProcessor.kt` |
| `DirectPendingAuthorizationMessageCoordinator` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/outgoing/DirectPendingAuthorizationMessageCoordinator.kt` |
| `DirectTargetDto` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/outgoing/DirectOutgoingMessageProcessor.kt` |
| `DirectConversationRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/repository/DirectConversationRepositoryImpl.kt` |
| `DirectMessageRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/repository/DirectMessageRepositoryImpl.kt` |
| `GroupAvatarBroadcaster` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/avatar/GroupAvatarBroadcaster.kt` |
| `GroupAvatarPacketProtocol` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/avatar/GroupAvatarPacketProtocol.kt` |
| `GroupAvatarDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupAvatarDataSource.kt` |
| `GroupAvatarFileDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupAvatarFileDataSource.kt` |
| `GroupConversationContextDto` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupConversationDataSource.kt` |
| `GroupConversationDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupConversationDataSource.kt` |
| `GroupConversationHistoryDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupConversationHistoryDataSource.kt` |
| `GroupDescriptionDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupDescriptionDataSource.kt` |
| `GroupIncomingConversationDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupIncomingConversationDataSource.kt` |
| `GroupLocalCleanupDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupLocalCleanupDataSource.kt` |
| `GroupOutgoingMessageDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupOutgoingMessageDataSource.kt` |
| `GroupPinDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupPinDataSource.kt` |
| `GroupRecipientDeliveryDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupRecipientDeliveryDataSource.kt` |
| `GroupTitleDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupTitleDataSource.kt` |
| `GroupTitleSnapshot` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupTitleDataSource.kt` |
| `GroupMessageDeliveryCoordinator` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/delivery/GroupMessageDeliveryCoordinator.kt` |
| `GroupOutboxDeliveryHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/delivery/GroupOutboxDeliveryHandler.kt` |
| `GroupDescriptionBroadcaster` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/description/GroupDescriptionBroadcaster.kt` |
| `GroupDescriptionPacketProtocol` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/description/GroupDescriptionPacketProtocol.kt` |
| `GroupCreatedIncomingProcessor` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/GroupCreatedIncomingProcessor.kt` |
| `GroupIncomingPacketPolicy` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/GroupIncomingPacketPolicy.kt` |
| `GroupIncomingPacketProcessor` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/GroupIncomingPacketProcessor.kt` |
| `GroupPacketHandlerRegistry` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/GroupPacketHandlerRegistry.kt` |
| `GroupWelcomePersistence` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/GroupWelcomePersistence.kt` |
| `PreviousGroupMembershipDto` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/GroupWelcomeModels.kt` |
| `GroupAvatarUpdatedPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/handler/GroupAvatarUpdatedPacketHandler.kt` |
| `GroupChatMessagePacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/handler/GroupChatMessagePacketHandler.kt` |
| `GroupDescriptionUpdatedPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/handler/GroupDescriptionUpdatedPacketHandler.kt` |
| `GroupMessageDeletionPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/handler/GroupMessageDeletionPacketHandler.kt` |
| `GroupMessageEditPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/handler/GroupMessageEditPacketHandler.kt` |
| `GroupPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/handler/GroupPacketHandler.kt` |
| `GroupPinUpdatedPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/handler/GroupPinUpdatedPacketHandler.kt` |
| `GroupReceiptPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/handler/GroupReceiptPacketHandler.kt` |
| `GroupTitleUpdatedPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/incoming/handler/GroupTitleUpdatedPacketHandler.kt` |
| `GroupLocalMembershipTimelineDto` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/mapper/GroupLocalMembershipTimelineDto.kt` |
| `GroupMembershipMessageFactory` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/mapper/GroupMembershipMessageFactory.kt` |
| `GroupOutgoingMessageProcessor` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/outgoing/GroupOutgoingMessageProcessor.kt` |
| `GroupPacketBroadcaster` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/outgoing/GroupPacketBroadcaster.kt` |
| `GroupPinBroadcaster` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/pin/GroupPinBroadcaster.kt` |
| `GroupPinPacketProtocol` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/pin/GroupPinPacketProtocol.kt` |
| `GroupAvatarRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupAvatarRepositoryImpl.kt` |
| `GroupConversationRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupConversationRepositoryImpl.kt` |
| `GroupDescriptionRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupDescriptionRepositoryImpl.kt` |
| `GroupKeyRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupKeyRepositoryImpl.kt` |
| `GroupMessageRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupMessageRepositoryImpl.kt` |
| `GroupPinRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupPinRepositoryImpl.kt` |
| `GroupTitleRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupTitleRepositoryImpl.kt` |
| `GroupVerificationRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupVerificationRepositoryImpl.kt` |
| `GroupTitleBroadcaster` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/title/GroupTitleBroadcaster.kt` |
| `GroupTitlePacketProtocol` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/title/GroupTitlePacketProtocol.kt` |
| `GroupVerificationDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/verification/GroupVerificationDataSource.kt` |
| `GroupVerificationPayloadEncoder` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/verification/GroupVerificationPayloadEncoder.kt` |
| `IncomingPacketProcessor` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/incoming/IncomingPacketProcessor.kt` |
| `IncomingPacketRouter` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/incoming/IncomingPacketRouter.kt` |
| `ReceiptIncomingPacketRouter` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/incoming/ReceiptIncomingPacketRouter.kt` |
| `DecodedIncomingPacketDto` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/model/DecodedIncomingPacketDto.kt` |
| `ImageVideoTypeDto` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/model/MessagePartDto.kt` |
| `MessagePartDto` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/model/MessagePartDto.kt` |
| `ChatsConversationPort` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/orchestration/ChatsConversationPort.kt` |
| `ChatOutboxDeliveryStateRouter` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/outbox/ChatOutboxDeliveryStateRouter.kt` |
| `ConversationOverviewDataSource` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/overview/datasource/ConversationOverviewDataSource.kt` |
| `ConversationOverviewRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/overview/repository/ConversationOverviewRepositoryImpl.kt` |
| `DirectIndicatorRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/repository/DirectIndicatorRepositoryImpl.kt` |
| `GroupIndicatorRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/repository/GroupIndicatorRepositoryImpl.kt` |
| `MessageHistoryRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/repository/MessageHistoryRepositoryImpl.kt` |
| `ForwardMessageContent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/ForwardMessageContent.kt` |
| `ForwardingTarget` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/ForwardingTarget.kt` |
| `ImageVideoType` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessagePart.kt` |
| `IndicatorType` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/IndicatorType.kt` |
| `LocationShareEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/LocationShareState.kt` |
| `LocationShareState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/LocationShareState.kt` |
| `LocationShareStateMachine` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/LocationShareState.kt` |
| `MessageComposerAvailability` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageComposerAvailability.kt` |
| `MessageComposerPolicy` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageComposerAvailability.kt` |
| `MessageContentStatus` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageContentStatus.kt` |
| `MessageDeliveryEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageDeliveryEvent.kt` |
| `MessageDeliveryStatus` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageDeliveryStatus.kt` |
| `MessageHistoryCursor` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageHistoryCursor.kt` |
| `MessageHistoryLoadResult` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageHistoryLoadResult.kt` |
| `MessageHistoryPolicy` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageHistoryPolicy.kt` |
| `MessagePart` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessagePart.kt` |
| `MessageReaction` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageReaction.kt` |
| `MessageSecurity` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageSecurity.kt` |
| `ContactSecurityState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/ContactSecurityState.kt` |
| `DirectChatContext` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectChatContext.kt` |
| `DirectComposerState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectComposerState.kt` |
| `DirectConversation` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectConversation.kt` |
| `DirectMessage` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectMessage.kt` |
| `DirectMessageDeliveryStateMachine` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectMessageDeliveryStateMachine.kt` |
| `DirectMessageDispatchResult` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectMessageDispatchResult.kt` |
| `DirectPendingAuthorizationMessagePolicy` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectPendingAuthorizationMessagePolicy.kt` |
| `ChatMessageType` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/ChatMessageType.kt` |
| `GroupAvatar` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupAvatar.kt` |
| `GroupAvatarMetadata` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupAvatarMetadata.kt` |
| `GroupChatContext` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupChatContext.kt` |
| `GroupComposerState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupComposerState.kt` |
| `GroupConversation` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupConversation.kt` |
| `GroupDescription` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupDescription.kt` |
| `GroupDetailsContext` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupDetailsContext.kt` |
| `GroupMessage` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupMessage.kt` |
| `GroupMessageDeliveryStateMachine` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupMessageDeliveryStateMachine.kt` |
| `GroupPin` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupPin.kt` |
| `GroupPinTarget` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupPinTarget.kt` |
| `GroupVerificationContext` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupVerificationState.kt` |
| `GroupVerificationMembershipStatus` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupVerificationState.kt` |
| `GroupVerificationPair` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupVerificationState.kt` |
| `GroupVerificationState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupVerificationState.kt` |
| `MessageDeliveryProgress` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/MessageDeliveryProgress.kt` |
| `ConversationOverview` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/overview/ConversationOverview.kt` |
| `ConversationOverviewContext` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/overview/ConversationOverviewContext.kt` |
| `ConversationOverviewPreview` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/overview/ConversationOverview.kt` |
| `ConversationOverviewType` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/overview/ConversationOverview.kt` |
| `MessageHistoryRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/MessageHistoryRepository.kt` |
| `DirectConversationRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/direct/DirectConversationRepository.kt` |
| `DirectIndicatorRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/direct/DirectIndicatorRepository.kt` |
| `DirectMessageRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/direct/DirectMessageRepository.kt` |
| `GroupAvatarRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupAvatarRepository.kt` |
| `GroupConversationRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupConversationRepository.kt` |
| `GroupDescriptionRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupDescriptionRepository.kt` |
| `GroupIndicatorRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupIndicatorRepository.kt` |
| `GroupKeyRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupKeyRepository.kt` |
| `GroupMessageRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupMessageRepository.kt` |
| `GroupPinRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupPinRepository.kt` |
| `GroupTitleRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupTitleRepository.kt` |
| `GroupVerificationActionRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupVerificationActionRepository.kt` |
| `GroupVerificationRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupVerificationRepository.kt` |
| `ConversationOverviewRepository` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/overview/ConversationOverviewRepository.kt` |
| `FindMessageHistoryCursorUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/FindMessageHistoryCursorUseCase.kt` |
| `EncodeContactForSharingUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/contact/EncodeContactForSharingUseCase.kt` |
| `ActivateAuthorizedDirectConversationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/ActivateAuthorizedDirectConversationUseCase.kt` |
| `DeleteDirectConversationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/DeleteDirectConversationUseCase.kt` |
| `DeleteDirectMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/DeleteDirectMessageUseCase.kt` |
| `DiscardPendingAuthorizationMessagesUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/DiscardPendingAuthorizationMessagesUseCase.kt` |
| `EditDirectMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/EditDirectMessageUseCase.kt` |
| `GetOrCreateDirectConversationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/GetOrCreateDirectConversationUseCase.kt` |
| `MarkDirectConversationReadUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/MarkDirectConversationReadUseCase.kt` |
| `ObserveDirectChatContextUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/ObserveDirectChatContextUseCase.kt` |
| `ObserveDirectConversationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/ObserveDirectConversationUseCase.kt` |
| `ObserveDirectIndicatorUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/ObserveDirectIndicatorUseCase.kt` |
| `QueueDirectMessageUntilAuthorizedUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/QueueDirectMessageUntilAuthorizedUseCase.kt` |
| `RetryDirectMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/RetryDirectMessageUseCase.kt` |
| `SendDirectMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/SendDirectMessageUseCase.kt` |
| `SendOrQueueDirectMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/SendOrQueueDirectMessageUseCase.kt` |
| `SetDirectIndicatorUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/SetDirectIndicatorUseCase.kt` |
| `ToggleDirectMessageReactionUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/ToggleDirectMessageReactionUseCase.kt` |
| `ForwardDirectMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/forward/ForwardDirectMessageUseCase.kt` |
| `ForwardMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/forward/ForwardMessageUseCase.kt` |
| `ForwardToContactUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/forward/ForwardToContactUseCase.kt` |
| `ForwardToDirectConversationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/forward/ForwardToDirectConversationUseCase.kt` |
| `ForwardToGroupConversationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/forward/ForwardToGroupConversationUseCase.kt` |
| `LoadOlderMessagesUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/forward/LoadOlderMessagesUseCase.kt` |
| `PrepareForwardMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/forward/PrepareForwardMessageUseCase.kt` |
| `AddGroupMembersUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/AddGroupMembersUseCase.kt` |
| `CreateGroupConversationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/CreateGroupConversationUseCase.kt` |
| `DeleteGroupMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/DeleteGroupMessageUseCase.kt` |
| `EditGroupMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/EditGroupMessageUseCase.kt` |
| `LoadGroupPinnedAttachmentUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/LoadGroupPinnedAttachmentUseCase.kt` |
| `MarkGroupConversationReadUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/MarkGroupConversationReadUseCase.kt` |
| `ObserveGroupChatContextUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupChatContextUseCase.kt` |
| `ObserveGroupConversationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupConversationUseCase.kt` |
| `ObserveGroupDetailsContextUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupDetailsContextUseCase.kt` |
| `ObserveGroupMemberIndicatorUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupMemberIndicatorUseCase.kt` |
| `ObserveGroupVerificationContextUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupVerificationContextUseCase.kt` |
| `ObserveGroupVerificationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupVerificationUseCase.kt` |
| `PinGroupMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/PinGroupMessageUseCase.kt` |
| `RemoveGroupAvatarUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/RemoveGroupAvatarUseCase.kt` |
| `RetryGroupMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/RetryGroupMessageUseCase.kt` |
| `SendGroupMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/SendGroupMessageUseCase.kt` |
| `SetGroupAvatarUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/SetGroupAvatarUseCase.kt` |
| `SetGroupDescriptionUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/SetGroupDescriptionUseCase.kt` |
| `SetGroupIndicatorUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/SetGroupIndicatorUseCase.kt` |
| `SetGroupTitleUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/SetGroupTitleUseCase.kt` |
| `SynchronizeGroupVerificationUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/SynchronizeGroupVerificationUseCase.kt` |
| `ToggleGroupMessageReactionUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ToggleGroupMessageReactionUseCase.kt` |
| `UnpinGroupMessageUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/UnpinGroupMessageUseCase.kt` |
| `VerifyGroupMemberUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/VerifyGroupMemberUseCase.kt` |
| `ObserveConversationOverviewContextUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/overview/ObserveConversationOverviewContextUseCase.kt` |
| `ObserveConversationOverviewsUseCase` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/overview/ObserveConversationOverviewsUseCase.kt` |
| `ContactsContent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/ContactsFlow.kt` |
| `ContactsFlowViewModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/ContactsFlowViewModel.kt` |
| `MorphingSendButtonShape` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/component/SendButtonShape.kt` |
| `ComposerAvailabilityUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/ComposerAvailabilityUi.kt` |
| `ComposerPreviewUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/ComposerPreviewUi.kt` |
| `IndicatorUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/IndicatorUiState.kt` |
| `IndicatorUiType` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/IndicatorUiState.kt` |
| `MessageComposerUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/MessageComposerUiState.kt` |
| `MessageInputActions` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/MessageInputActions.kt` |
| `MessageInputState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/MessageInputState.kt` |
| `HeaderAvatarKind` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/header/model/HeaderUiModel.kt` |
| `HeaderUiModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/header/model/HeaderUiModel.kt` |
| `SecurityBannerState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/header/model/SecurityBannerState.kt` |
| `BubbleState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageBubble.kt` |
| `BurstItemProperties` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageReactionBurstOverlay.kt` |
| `DissolvingMessageListState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageDissolve.kt` |
| `MessageBubbleShape` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageBubbleShape.kt` |
| `PrimaryContent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageBubble.kt` |
| `DeliveryProgressUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageBubbleUi.kt` |
| `HistoryUiModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/HistoryUiModel.kt` |
| `ImageVideoTypeUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessagePartUi.kt` |
| `MessageBubbleUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageBubbleUi.kt` |
| `MessageContextAnchor` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageContextAnchor.kt` |
| `MessageContextUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageContextUiState.kt` |
| `MessageHistoryUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageHistoryUiState.kt` |
| `MessageJumpState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageSearchUiState.kt` |
| `MessagePartUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessagePartUi.kt` |
| `MessageReactionBurst` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageReactionBurst.kt` |
| `MessageReactionUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageBubbleUi.kt` |
| `MessageReplyUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageBubbleUi.kt` |
| `MessageSearchTargetState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageSearchUiState.kt` |
| `CreateGroupViewModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/CreateGroupViewModel.kt` |
| `ContactsFlowUiEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/ContactsFlowUiEvent.kt` |
| `CreateGroupConversationUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupUiState.kt` |
| `CreateGroupEffect` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupEffect.kt` |
| `CreateGroupUiEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupUiEvent.kt` |
| `DetailsContent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupDetailsFlow.kt` |
| `GroupDetailsPreviewData` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupDetailsPreviewData.kt` |
| `GroupVerificationViewModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupVerificationViewModel.kt` |
| `AddGroupMembersUiEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `DetailsTarget` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/DetailsTarget.kt` |
| `GroupAvatarUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupAvatarUiState.kt` |
| `GroupDescriptionUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDescriptionUiState.kt` |
| `GroupDetailsUiEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `GroupDetailsUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiState.kt` |
| `GroupLeavePrompt` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupLeaveUiState.kt` |
| `GroupLeaveUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupLeaveUiState.kt` |
| `GroupMemberManagementUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupMemberManagementUiState.kt` |
| `GroupMemberVerificationState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupVerificationSummaryUiState.kt` |
| `GroupMemberVerificationUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupVerificationSummaryUiState.kt` |
| `GroupTitleUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupTitleUiState.kt` |
| `GroupVerificationSummaryUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupVerificationSummaryUiState.kt` |
| `GroupVerificationUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupVerificationUiState.kt` |
| `DirectConversationViewModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/DirectConversationViewModel.kt` |
| `IndicatorController` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/IndicatorController.kt` |
| `DirectConversationUiEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `DirectConversationUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiState.kt` |
| `ForwardingSelectionViewModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/ForwardingSelectionViewModel.kt` |
| `ForwardingSelectionEffect` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionEffect.kt` |
| `ForwardingSelectionUiEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionUiEvent.kt` |
| `ForwardingSelectionUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionUiState.kt` |
| `ForwardingTargetUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingTargetUi.kt` |
| `AttachmentSelection` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationScreen.kt` |
| `GroupConversationViewModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationViewModel.kt` |
| `IndicatorController` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/IndicatorController.kt` |
| `GroupConversationUiEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `GroupConversationUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiState.kt` |
| `GroupMemberProgressUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupMembershipUiState.kt` |
| `GroupMembershipUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupMembershipUiState.kt` |
| `GroupMessageUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupMessageUi.kt` |
| `OverviewViewModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/OverviewViewModel.kt` |
| `ConversationListItem` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/ConversationListItem.kt` |
| `LastMessagePreviewUi` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/ConversationListItem.kt` |
| `OverviewUiEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/OverviewUiEvent.kt` |
| `OverviewUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/OverviewUiState.kt` |
| `GroupMemberQrVerificationViewModel` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/GroupMemberQrVerificationViewModel.kt` |
| `GroupMemberQrVerificationError` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiState.kt` |
| `GroupMemberQrVerificationUiEvent` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiEvent.kt` |
| `GroupMemberQrVerificationUiState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiState.kt` |
| `GroupVerificationReceiptPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/runtime/group/incoming/GroupVerificationReceiptPacketHandler.kt` |
| `GroupVerificationSnapshotPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/runtime/group/incoming/GroupVerificationSnapshotPacketHandler.kt` |
| `GroupVerificationSnapshotRequestPacketHandler` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/runtime/group/incoming/GroupVerificationSnapshotRequestPacketHandler.kt` |
| `GroupVerificationActionRepositoryImpl` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/runtime/group/verification/GroupVerificationActionRepositoryImpl.kt` |
| `GroupVerificationCoordinator` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/runtime/group/verification/GroupVerificationCoordinator.kt` |
| `GroupVerificationSnapshotSender` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/runtime/group/verification/GroupVerificationSnapshotSender.kt` |
| `GroupVerificationState` | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/runtime/group/verification/GroupVerificationState.kt` |

## `:feature:conversationorchestration`

Path: `feature/conversationorchestration`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:crypto`, `:core:protocol`, `:feature:contacts`, `:feature:identity`, `:feature:invite`, `:feature:membership`, `:feature:messaging`, `:feature:transport`

Production top-level declarations: **57**

| Class/type | Source set | File |
|---|---|---|
| `WebSocketIncomingEnvelopeGateway` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/data/datasource/WebSocketIncomingEnvelopeGateway.kt` |
| `DirectChatAuthorizationRequiredException` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/error/DirectChatAuthorizationRequiredException.kt` |
| `RemoteIdentityReplacementRequiredException` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/error/RemoteIdentityReplacementRequiredException.kt` |
| `ConversationMessagePlan` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/model/ConversationMessagePlan.kt` |
| `ConversationPort` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/port/ConversationPort.kt` |
| `AddConversationMembersUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/AddConversationMembersUseCase.kt` |
| `CreateConversationGroupUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/CreateConversationGroupUseCase.kt` |
| `DeleteConversationGroupUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/DeleteConversationGroupUseCase.kt` |
| `DeletePeerConversationUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/DeletePeerConversationUseCase.kt` |
| `GetConversationGroupLeaveRequirementUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/GetConversationGroupLeaveRequirementUseCase.kt` |
| `GroupVerificationInputsUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/GroupVerificationInputsUseCase.kt` |
| `LeaveConversationGroupUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/LeaveConversationGroupUseCase.kt` |
| `ObserveConversationIndicatorUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ObserveConversationIndicatorUseCase.kt` |
| `ObserveConversationQueueAvailabilityUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ObserveConversationQueueAvailabilityUseCase.kt` |
| `OwnedGroupVerificationInputs` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/GroupVerificationInputsUseCase.kt` |
| `PendingGroupIdentity` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/GroupVerificationInputsUseCase.kt` |
| `PrepareConversationMessageUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/PrepareConversationMessageUseCase.kt` |
| `PrepareConversationOpenUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/PrepareConversationOpenUseCase.kt` |
| `PromoteConversationGroupMemberUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/PromoteConversationGroupMemberUseCase.kt` |
| `ReconnectExistingConversationUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ReconnectExistingConversationUseCase.kt` |
| `RemoveConversationGroupMemberUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/RemoveConversationGroupMemberUseCase.kt` |
| `RequireDirectChatAuthorizationUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/RequireDirectChatAuthorizationUseCase.kt` |
| `ResolveIncomingIdentityPeerUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ResolveIncomingIdentityPeerUseCase.kt` |
| `ResolveSigningIdentityContactUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ResolveSigningIdentityContactUseCase.kt` |
| `SendConversationIndicatorUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/SendConversationIndicatorUseCase.kt` |
| `StartRecoveryInvitationUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/StartRecoveryInvitationUseCase.kt` |
| `TransferConversationGroupAdminAndLeaveUseCase` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/TransferConversationGroupAdminAndLeaveUseCase.kt` |
| `ConversationFlowHandler` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/workflow/ConversationFlowHandler.kt` |
| `GroupInvitationIdentityDisposition` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/workflow/GroupInvitationIdentityPolicy.kt` |
| `GroupInvitationIdentityPolicy` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/workflow/GroupInvitationIdentityPolicy.kt` |
| `IncomingAuthorizationRevocationDecision` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/workflow/IncomingAuthorizationRevocationDecision.kt` |
| `ApprovedIdentityReconnectionObserver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/ApprovedIdentityReconnectionObserver.kt` |
| `ApprovedReconnectionRetryWorker` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/ApprovedReconnectionRetryWorker.kt` |
| `ContactBlockObserver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/ContactBlockObserver.kt` |
| `GroupMembershipPacketObserver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/GroupMembershipPacketObserver.kt` |
| `IdentityExchangePacketObserver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/IdentityExchangePacketObserver.kt` |
| `IdentityResultObserver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/IdentityResultObserver.kt` |
| `InvitationResultObserver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/InvitationResultObserver.kt` |
| `MembershipResultObserver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/MembershipResultObserver.kt` |
| `MessagingTransportResultObserver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/MessagingTransportResultObserver.kt` |
| `DefaultIncomingEnvelopeProcessor` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/incoming/DefaultIncomingEnvelopeProcessor.kt` |
| `WebSocketMessagingIndicatorGateway` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/indicator/WebSocketMessagingIndicatorGateway.kt` |
| `DefaultMailboxCapabilityLifecycle` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/DefaultMailboxCapabilityLifecycle.kt` |
| `DefaultMailboxCoordinator` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/DefaultMailboxCoordinator.kt` |
| `MailboxCredentialFactory` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/MailboxCredentialFactory.kt` |
| `MailboxPendingSynchronizer` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/MailboxPendingSynchronizer.kt` |
| `MailboxRoutePacketHandler` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/MailboxRoutePacketHandler.kt` |
| `MailboxRouteProvisioner` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/MailboxRouteProvisioner.kt` |
| `InvitationTransportFailureHandler` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/InvitationTransportFailureHandler.kt` |
| `OutgoingPacketSender` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingPacketSender.kt` |
| `OutgoingPacketTransportPolicy` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingPacketTransportPolicy.kt` |
| `OutgoingRecipientRoutingResolver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingRecipientRoutingResolver.kt` |
| `OutgoingTransportPayloadFactory` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingTransportPayloadFactory.kt` |
| `OutgoingTransportRequirement` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingTransportRequirement.kt` |
| `RetryableWireDeliveryException` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/RetryableWireDeliveryException.kt` |
| `GroupRoutingResolver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/routing/GroupRoutingResolver.kt` |
| `GroupTransportKeyResolver` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/routing/GroupTransportKeyResolver.kt` |

## `:feature:membership`

Path: `feature/membership`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:crypto`, `:core:protocol`, `:data:database`, `:data:datastore`

Production top-level declarations: **125**

| Class/type | Source set | File |
|---|---|---|
| `AndroidGroupKeyStore` | `androidMain` | `feature/membership/src/androidMain/kotlin/com/cbgm/sparrow/feature/membership/device/AndroidGroupKeyStore.kt` |
| `GroupMembershipEvent` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipStateMachine.kt` |
| `GroupMembershipLock` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipLock.kt` |
| `GroupMembershipStateMachine` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipStateMachine.kt` |
| `GroupEpochDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupEpochDataSource.kt` |
| `GroupEpochSecurityDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupEpochSecurityDataSource.kt` |
| `GroupIncomingActivationDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingActivationDataSource.kt` |
| `GroupIncomingDeletionDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingDeletionDataSource.kt` |
| `GroupIncomingRemovalDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingRemovalDataSource.kt` |
| `GroupIncomingWelcomeDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingWelcomeDataSource.kt` |
| `GroupLeaveDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupLeaveDataSource.kt` |
| `GroupMemberPromotionDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMemberPromotionDataSource.kt` |
| `GroupMemberRemovalDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMemberRemovalDataSource.kt` |
| `GroupMembershipActivationDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipActivationDataSource.kt` |
| `GroupMembershipAdministrationDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipAdministrationDataSource.kt` |
| `GroupMembershipDeletionDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipDeletionDataSource.kt` |
| `GroupMembershipLifecycleDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipLifecycleDataSource.kt` |
| `GroupMembershipStoreDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipStoreDataSource.kt` |
| `GroupOwnerWelcomeDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupOwnerWelcomeDataSource.kt` |
| `GroupPacketBroadcaster` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupPacketBroadcaster.kt` |
| `GroupReadyAcknowledgementDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupReadyAcknowledgementDataSource.kt` |
| `GroupSecurityStoreDataSource` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupSecurityStoreDataSource.kt` |
| `CreatedGroupSecurityDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipSecurityDto.kt` |
| `GroupConversationStateDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupInvitationDirection` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupInvitationStatus.kt` |
| `GroupInvitationStatus` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupInvitationStatus.kt` |
| `GroupLeaveRequirementDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupLocalMembershipEndDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupLocalMembershipEndDto.kt` |
| `GroupMemberProgressDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupMemberProgressStatusDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupMembershipParticipantDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipParticipantDto.kt` |
| `GroupMembershipPeerDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipPeerDto.kt` |
| `GroupMembershipPerspective` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipStatus.kt` |
| `GroupMembershipStatus` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipStatus.kt` |
| `GroupWelcomeRecipientDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipSecurityDto.kt` |
| `GroupMembershipPacketProtocol` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/protocol/GroupMembershipPacketProtocol.kt` |
| `GroupMembershipPayloadEncoder` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/protocol/GroupMembershipPayloadEncoder.kt` |
| `GroupMembershipRepositoryImpl` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/repository/GroupMembershipRepositoryImpl.kt` |
| `MembershipRepositoryImpl` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/repository/MembershipRepositoryImpl.kt` |
| `GroupSecurityManager` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupSecurityManager.kt` |
| `GroupWelcomeSecurity` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupWelcomeSecurity.kt` |
| `GroupAdministrationState` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupAdministrationState.kt` |
| `GroupConversationMembershipProjection` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipProjection.kt` |
| `GroupConversationMembershipProjector` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipProjection.kt` |
| `GroupConversationMembershipSnapshot` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipSnapshot.kt` |
| `GroupConversationState` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationState.kt` |
| `GroupIncomingWelcomeAuthorization` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupIncomingWelcomeAuthorization.kt` |
| `GroupLeaveRequirement` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupLeaveRequirement.kt` |
| `GroupLocalMembershipEnd` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupLocalMembershipEnd.kt` |
| `GroupMemberLifecycleSnapshot` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipSnapshot.kt` |
| `GroupMemberProgress` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberProgress.kt` |
| `GroupMemberProgressStatus` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberProgress.kt` |
| `GroupMemberPromotionResult` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberAdministrationResult.kt` |
| `GroupMemberRemovalReason` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberAdministrationResult.kt` |
| `GroupMemberRemovalResult` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberAdministrationResult.kt` |
| `GroupMembershipContext` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMembershipContext.kt` |
| `GroupMessageMembershipAccess` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMessageMembershipAccess.kt` |
| `GroupMetadataMessageSender` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMetadataMessageSender.kt` |
| `GroupMetadataSendContext` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMetadataSecurityContext.kt` |
| `GroupTransportRoutingMember` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupTransportRoutingMember.kt` |
| `GroupVerificationMemberKey` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupVerificationMembership` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupVerificationMembershipContext` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupVerificationSecurityState` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupWelcomeMemberKey` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupWelcomeMemberKey.kt` |
| `IncomingMembershipOffer` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipDeclineDisposition` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipDeclineResult` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipHandshake` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipJoinRequest` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipPerspective` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipStatus.kt` |
| `MembershipResult` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipResult.kt` |
| `MembershipSigningProof` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipStatus` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipStatus.kt` |
| `MembershipVerificationSnapshot` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipVerificationSnapshot.kt` |
| `OpenedGroupWelcomeDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupSecurityModels.kt` |
| `OpenedIncomingGroupWelcome` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/OpenedIncomingGroupWelcome.kt` |
| `SecuredGroupMessageDto` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupSecurityModels.kt` |
| `StartedMembershipHandshake` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `GroupMembershipRepository` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/repository/GroupMembershipRepository.kt` |
| `GroupSecurityRepository` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/repository/GroupSecurityRepository.kt` |
| `MembershipRepository` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/repository/MembershipRepository.kt` |
| `AcceptGroupMembershipUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AcceptGroupMembershipUseCase.kt` |
| `ApplyIncomingGroupActivationUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ApplyIncomingGroupActivationUseCase.kt` |
| `AuthorizeGroupMetadataUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeGroupMetadataUseCase.kt` |
| `AuthorizeIncomingGroupActivationUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupActivationUseCase.kt` |
| `AuthorizeIncomingGroupDeletionUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupDeletionUseCase.kt` |
| `AuthorizeIncomingGroupRemovalUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupRemovalUseCase.kt` |
| `AuthorizeIncomingGroupWelcomeUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupWelcomeUseCase.kt` |
| `ClearMembershipHandshakeUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ClearMembershipHandshakeUseCase.kt` |
| `CompleteIncomingGroupDeletionUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/CompleteIncomingGroupDeletionUseCase.kt` |
| `CompleteIncomingGroupRemovalUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/CompleteIncomingGroupRemovalUseCase.kt` |
| `CompleteIncomingGroupWelcomeUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/CompleteIncomingGroupWelcomeUseCase.kt` |
| `ConfirmGroupMembershipIdentityUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ConfirmGroupMembershipIdentityUseCase.kt` |
| `DeclineGroupMembershipUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/DeclineGroupMembershipUseCase.kt` |
| `DeleteGroupMembershipUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/DeleteGroupMembershipUseCase.kt` |
| `DiscardSupersededMembershipsUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/DiscardSupersededMembershipsUseCase.kt` |
| `GetGroupCurrentEpochUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupCurrentEpochUseCase.kt` |
| `GetGroupLeaveRequirementUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupLeaveRequirementUseCase.kt` |
| `GetGroupMessageMembershipAccessUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupMessageMembershipAccessUseCase.kt` |
| `GetGroupPinSenderSigningKeyUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupPinSenderSigningKeyUseCase.kt` |
| `GetGroupTransportRoutingMembersUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupTransportRoutingMembersUseCase.kt` |
| `GetMembershipHandshakeUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetMembershipHandshakeUseCase.kt` |
| `InspectIncomingGroupMembershipUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/InspectIncomingGroupMembershipUseCase.kt` |
| `LeaveGroupUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/LeaveGroupUseCase.kt` |
| `MarkMembershipRemovedUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/MarkMembershipRemovedUseCase.kt` |
| `ObserveGroupAdministrationUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ObserveGroupAdministrationUseCase.kt` |
| `ObserveMembershipResultsUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ObserveMembershipResultsUseCase.kt` |
| `OpenIncomingGroupWelcomeUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/OpenIncomingGroupWelcomeUseCase.kt` |
| `PersistIncomingGroupWelcomeUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/PersistIncomingGroupWelcomeUseCase.kt` |
| `PromoteGroupMemberUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/PromoteGroupMemberUseCase.kt` |
| `ReceiveGroupActivationAcknowledgementUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupActivationAcknowledgementUseCase.kt` |
| `ReceiveGroupLeaveRequestUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupLeaveRequestUseCase.kt` |
| `ReceiveGroupMembershipDeclineUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupMembershipDeclineUseCase.kt` |
| `ReceiveGroupMembershipJoinRequestUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupMembershipJoinRequestUseCase.kt` |
| `ReceiveGroupMembershipReceiptUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupMembershipReceiptUseCase.kt` |
| `ReceiveGroupReadyAcknowledgementUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupReadyAcknowledgementUseCase.kt` |
| `ReceiveIncomingGroupMembershipUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveIncomingGroupMembershipUseCase.kt` |
| `RemoveGroupMemberUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/RemoveGroupMemberUseCase.kt` |
| `ResolveGroupTransportEncryptionPublicKeyUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ResolveGroupTransportEncryptionPublicKeyUseCase.kt` |
| `SendGroupReadyAcknowledgementUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/SendGroupReadyAcknowledgementUseCase.kt` |
| `StartGroupMembershipUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/StartGroupMembershipUseCase.kt` |
| `TransferGroupAdminAndLeaveUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/TransferGroupAdminAndLeaveUseCase.kt` |
| `VerifyGroupKeyConfirmationUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/VerifyGroupKeyConfirmationUseCase.kt` |
| `WasGroupMembershipDeletedUseCase` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/WasGroupMembershipDeletedUseCase.kt` |

## `:feature:attachments`

Path: `feature/attachments`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:data:database`, `:feature:media`, `:feature:transport`

Production top-level declarations: **66**

| Class/type | Source set | File |
|---|---|---|
| `AndroidLocationOpener` | `androidMain` | `feature/attachments/src/androidMain/kotlin/com/cbgm/sparrow/feature/attachments/device/LocationOpener.android.kt` |
| `AttachmentContentDataSource` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/AttachmentContentDataSource.kt` |
| `BlobTransferDataSource` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/BlobTransferDataSource.kt` |
| `LocalAttachmentContentDataSource` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/LocalAttachmentContentDataSource.kt` |
| `LocalAttachmentDataSource` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/LocalAttachmentDataSource.kt` |
| `MessageAttachmentDataSource` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/MessageAttachmentDataSource.kt` |
| `MessageAttachmentFileDataSource` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/MessageAttachmentFileDataSource.kt` |
| `AttachmentContentPayloadDto` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentContentPayloadDto.kt` |
| `AttachmentMessageContextDto` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentMessageContextDto.kt` |
| `AttachmentStorageSummaryDto` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentStorageSummaryDto.kt` |
| `AttachmentTargetDto` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentTargetDto.kt` |
| `OutgoingMessageAttachmentDto` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/OutgoingMessageAttachmentDto.kt` |
| `PreparedMessageAttachmentDto` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/PreparedMessageAttachmentDto.kt` |
| `UploadedBlobDto` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/UploadedBlobDto.kt` |
| `BlobTransferRepositoryImpl` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/repository/BlobTransferRepositoryImpl.kt` |
| `MessageAttachmentOperationsRepositoryImpl` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/repository/MessageAttachmentOperationsRepositoryImpl.kt` |
| `MessageAttachmentRepositoryImpl` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/repository/MessageAttachmentRepositoryImpl.kt` |
| `CurrentLocationLauncher` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/device/CurrentLocationLauncher.kt` |
| `LocationOpener` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/device/LocationOpener.kt` |
| `AttachmentContent` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentContent.kt` |
| `AttachmentMessageContext` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentMessageContext.kt` |
| `AttachmentSource` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTarget.kt` |
| `AttachmentStorageSummary` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentStorageSummary.kt` |
| `AttachmentTarget` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTarget.kt` |
| `AttachmentTranscript` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTranscript.kt` |
| `AttachmentTranscriptCue` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTranscript.kt` |
| `CurrentLocation` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/CurrentLocation.kt` |
| `LocalAttachment` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/LocalAttachment.kt` |
| `MessageAttachment` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/MessageAttachment.kt` |
| `MessageAttachmentPolicy` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/MessageAttachmentPolicy.kt` |
| `OutgoingMessageAttachment` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/OutgoingMessageAttachment.kt` |
| `PreparedMessageAttachment` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/PreparedMessageAttachment.kt` |
| `SharedContact` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/SharedContact.kt` |
| `UploadedBlob` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/UploadedBlob.kt` |
| `BlobTransferRepository` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/repository/BlobTransferRepository.kt` |
| `MessageAttachmentOperationsRepository` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/repository/MessageAttachmentOperationsRepository.kt` |
| `MessageAttachmentRepository` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/repository/MessageAttachmentRepository.kt` |
| `DeleteBlobUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/DeleteBlobUseCase.kt` |
| `DeleteConversationLocalAttachmentsUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/DeleteConversationLocalAttachmentsUseCase.kt` |
| `DeleteLocalAttachmentsUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/DeleteLocalAttachmentsUseCase.kt` |
| `DownloadBlobUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/DownloadBlobUseCase.kt` |
| `LoadAttachmentBytesUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/LoadAttachmentBytesUseCase.kt` |
| `LoadAttachmentContentUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/LoadAttachmentContentUseCase.kt` |
| `LoadMessageAttachmentUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/LoadMessageAttachmentUseCase.kt` |
| `ObserveAttachmentStorageSummariesUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/ObserveAttachmentStorageSummariesUseCase.kt` |
| `ObserveLocalAttachmentsUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/ObserveLocalAttachmentsUseCase.kt` |
| `ObserveMessageAttachmentTranscriptUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/ObserveMessageAttachmentTranscriptUseCase.kt` |
| `SaveMessageAttachmentTranscriptUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/SaveMessageAttachmentTranscriptUseCase.kt` |
| `UploadBlobUseCase` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/UploadBlobUseCase.kt` |
| `AttachmentViewModel` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/AttachmentViewModel.kt` |
| `AttachmentManagementLocalState` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/AttachmentManagementViewModel.kt` |
| `AttachmentManagementViewModel` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/AttachmentManagementViewModel.kt` |
| `AttachmentSnapshot` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/AttachmentManagementViewModel.kt` |
| `AttachmentManagementTab` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementTab.kt` |
| `AttachmentManagementUiEvent` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `AttachmentManagementUiState` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiState.kt` |
| `AttachmentUiState` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/model/AttachmentUiState.kt` |
| `MessageAttachmentUi` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/model/MessageAttachmentUi.kt` |
| `AttachmentStorageViewModel` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/AttachmentStorageViewModel.kt` |
| `AttachmentStorageUiEvent` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiEvent.kt` |
| `AttachmentStorageUiState` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiState.kt` |
| `MessageAttachmentCacheCoordinator` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/runtime/MessageAttachmentCacheCoordinator.kt` |
| `ContactAttachmentPayload` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/util/ContactAttachmentPayload.kt` |
| `LocationAttachmentPayload` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/util/LocationAttachmentPayload.kt` |
| `CurrentLocationDelegate` | `iosMain` | `feature/attachments/src/iosMain/kotlin/com/cbgm/sparrow/feature/attachments/device/CurrentLocationLauncher.ios.kt` |
| `IosLocationOpener` | `iosMain` | `feature/attachments/src/iosMain/kotlin/com/cbgm/sparrow/feature/attachments/device/LocationOpener.ios.kt` |

## `:feature:media`

Path: `feature/media`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:ui`

Production top-level declarations: **66**

| Class/type | Source set | File |
|---|---|---|
| `AndroidFileBrowserDataSource` | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/AndroidFileBrowserDataSource.kt` |
| `AndroidFileOpener` | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.android.kt` |
| `AndroidMediaExporter` | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/MediaExport.android.kt` |
| `AndroidMediaSelectionFileDataSource` | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/AndroidMediaSelectionFileDataSource.kt` |
| `FileBrowserDataSource` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/datasource/FileBrowserDataSource.kt` |
| `MediaSelectionFileDataSource` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/datasource/MediaSelectionFileDataSource.kt` |
| `FileBrowserContentDto` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/FileBrowserContentDto.kt` |
| `FileBrowserDirectoryDto` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/FileBrowserDirectoryDto.kt` |
| `FileBrowserEntryDto` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/FileBrowserEntryDto.kt` |
| `StoredMediaSelectionPathsDto` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/StoredMediaSelectionPathsDto.kt` |
| `FileBrowserRepositoryImpl` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/repository/FileBrowserRepositoryImpl.kt` |
| `MediaSelectionFileRepositoryImpl` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/repository/MediaSelectionFileRepositoryImpl.kt` |
| `CameraCaptureLauncher` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/CameraCapture.kt` |
| `FileAccessLauncher` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/FileAccessLauncher.kt` |
| `FileOpener` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.kt` |
| `GalleryPickerLauncher` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.kt` |
| `GalleryPickerStrings` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.kt` |
| `MediaExporter` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/MediaExport.kt` |
| `CameraCaptureConfig` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CameraCaptureType` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CameraLens` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CapturedMedia` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `FileBrowserContent` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/FileBrowserContent.kt` |
| `FileBrowserDirectory` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/FileBrowserDirectory.kt` |
| `FileBrowserEntry` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/FileBrowserEntry.kt` |
| `GalleryMedia` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/GalleryMedia.kt` |
| `GalleryPickerConfig` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/GalleryMedia.kt` |
| `MediaContentType` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaContentType.kt` |
| `MediaExportItem` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaExportItem.kt` |
| `StoredMediaSelectionPaths` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/StoredMediaSelectionPaths.kt` |
| `FileBrowserRepository` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/repository/FileBrowserRepository.kt` |
| `MediaSelectionFileRepository` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/repository/MediaSelectionFileRepository.kt` |
| `BrowseFileDirectoryUseCase` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/BrowseFileDirectoryUseCase.kt` |
| `CheckFileBrowserAccessUseCase` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/CheckFileBrowserAccessUseCase.kt` |
| `GetFileBrowserRootUseCase` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/GetFileBrowserRootUseCase.kt` |
| `ReadFileBrowserEntryUseCase` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/ReadFileBrowserEntryUseCase.kt` |
| `SetFileBrowserRootUseCase` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/SetFileBrowserRootUseCase.kt` |
| `DirectoryState` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerViewModel.kt` |
| `FilePickerLauncher` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerLauncher.kt` |
| `FilePickerSession` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerSessionController.kt` |
| `FilePickerSessionController` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerSessionController.kt` |
| `FilePickerSessionSnapshot` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerSessionController.kt` |
| `FilePickerViewModel` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerViewModel.kt` |
| `FileBrowserEntryKind` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FileBrowserEntryUi` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FilePickerBreadcrumbUi` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FilePickerSessionResult` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerSessionResult.kt` |
| `FilePickerSortMode` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FilePickerUiEvent` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `FilePickerUiState` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `MediaItem` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaItem.kt` |
| `MediaSelection` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelection.kt` |
| `MediaSelectionResult` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionResult.kt` |
| `MediaSelectionSource` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelection.kt` |
| `MediaSelectionType` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelection.kt` |
| `MediaType` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaItem.kt` |
| `MediaSelectionLauncher` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/selection/MediaSelectionLauncher.kt` |
| `CameraPickerDelegate` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/CameraCapture.ios.kt` |
| `DocumentInteractionDelegate` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.ios.kt` |
| `FileAccessDelegate` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/FileAccessLauncher.ios.kt` |
| `GalleryPickerDelegate` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.ios.kt` |
| `GallerySelectionCollector` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.ios.kt` |
| `IosFileBrowserDataSource` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/IosFileBrowserDataSource.kt` |
| `IosFileOpener` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.ios.kt` |
| `IosMediaSelectionFileDataSource` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/IosMediaSelectionFileDataSource.kt` |
| `VideoView` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/PlatformVideoMedia.ios.kt` |

## `:feature:voice`

Path: `feature/voice`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:protocol`, `:core:ui`, `:data:datastore`, `:feature:attachments`

Production top-level declarations: **51**

| Class/type | Source set | File |
|---|---|---|
| `AndroidVoicePlayer` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoicePlayer.kt` |
| `AndroidVoiceRecorder` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoiceRecorder.kt` |
| `AndroidVoiceTranscriptionRepository` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoiceTranscriptionRepository.kt` |
| `AndroidWhisperModelStore` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidWhisperModelStore.kt` |
| `ByteArrayMediaDataSource` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoicePlayer.kt` |
| `WhisperNative` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/WhisperNative.kt` |
| `VoiceTranscriptionSettingsDataSource` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/data/datasource/VoiceTranscriptionSettingsDataSource.kt` |
| `VoiceRepositoryImpl` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/data/repository/VoiceRepositoryImpl.kt` |
| `VoiceTranscriptionSettingsRepositoryImpl` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/data/repository/VoiceTranscriptionSettingsRepositoryImpl.kt` |
| `PcmWaveAudio` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/device/PcmWaveAudio.kt` |
| `VoicePlayer` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/device/VoicePlayer.kt` |
| `VoiceRecorder` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/device/VoiceRecorder.kt` |
| `VoiceComposerPhase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceComposerState` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceMessageTarget` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceMessageTarget.kt` |
| `VoicePlaybackState` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceRecording` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceRecording.kt` |
| `VoiceTranscript` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceTranscript.kt` |
| `VoiceTranscriptCue` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceTranscript.kt` |
| `VoiceTranscriptionState` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceRepository` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/repository/VoiceRepository.kt` |
| `VoiceTranscriptionRepository` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/repository/VoiceTranscriptionRepository.kt` |
| `VoiceTranscriptionSettingsRepository` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/repository/VoiceTranscriptionSettingsRepository.kt` |
| `CancelVoiceRecordingUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/CancelVoiceRecordingUseCase.kt` |
| `FinishVoiceMessageScrubUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/FinishVoiceMessageScrubUseCase.kt` |
| `GetRecordedVoiceAttachmentUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/GetRecordedVoiceAttachmentUseCase.kt` |
| `ObserveVoiceComposerUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoiceComposerUseCase.kt` |
| `ObserveVoicePlaybackUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoicePlaybackUseCase.kt` |
| `ObserveVoiceRecordingActiveUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoiceRecordingActiveUseCase.kt` |
| `ObserveVoiceTranscriptionEnabledUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoiceTranscriptionEnabledUseCase.kt` |
| `PrepareVoiceTranscriptionUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/PrepareVoiceTranscriptionUseCase.kt` |
| `ResetVoiceComposerUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ResetVoiceComposerUseCase.kt` |
| `SetVoiceTranscriptionEnabledUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/SetVoiceTranscriptionEnabledUseCase.kt` |
| `StartVoiceMessageScrubUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/StartVoiceMessageScrubUseCase.kt` |
| `StartVoiceRecordingUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/StartVoiceRecordingUseCase.kt` |
| `StopVoiceRecordingUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/StopVoiceRecordingUseCase.kt` |
| `ToggleVoiceMessagePlaybackUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ToggleVoiceMessagePlaybackUseCase.kt` |
| `ToggleVoicePreviewUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ToggleVoicePreviewUseCase.kt` |
| `TranscribeVoiceAudioUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceAudioUseCase.kt` |
| `TranscribeVoiceMessageUseCase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceMessageUseCase.kt` |
| `VoiceTranscriptionPhase` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceMessageUseCase.kt` |
| `VoiceTranscriptionPhaseException` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceMessageUseCase.kt` |
| `VoiceAction` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/composer/VoiceComposer.kt` |
| `VoiceComposerViewModel` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/composer/VoiceComposerViewModel.kt` |
| `VoiceComposerUiState` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/composer/model/VoiceComposerUiState.kt` |
| `VoiceMessageViewModel` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/message/VoiceMessageViewModel.kt` |
| `VoiceScrubState` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/message/VoiceMessageContent.kt` |
| `VoiceMessageUiState` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/message/model/VoiceMessageUiState.kt` |
| `IosVoicePlayer` | `iosMain` | `feature/voice/src/iosMain/kotlin/com/cbgm/sparrow/feature/voice/device/IosVoicePlayer.kt` |
| `IosVoiceRecorder` | `iosMain` | `feature/voice/src/iosMain/kotlin/com/cbgm/sparrow/feature/voice/device/IosVoiceRecorder.kt` |
| `IosVoiceTranscriptionRepository` | `iosMain` | `feature/voice/src/iosMain/kotlin/com/cbgm/sparrow/feature/voice/device/IosVoiceTranscriptionRepository.kt` |

## `:core:crypto`

Path: `core/crypto`  
Direct project dependencies detected from `build.gradle.kts`: none

Production top-level declarations: **43**

| Class/type | Source set | File |
|---|---|---|
| `InitializeCryptoRuntime` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/InitializeCryptoRuntime.kt` |
| `SodiumRuntime` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/SodiumRuntime.kt` |
| `BlobCipher` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/blob/BlobCipher.kt` |
| `EncryptedBlob` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/blob/BlobCipher.kt` |
| `SodiumBlobCipher` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/blob/SodiumBlobCipher.kt` |
| `CryptoException` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `CryptoNotInitializedException` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `InvalidPrivateKeyException` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `InvalidPublicKeyException` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `MessageDecryptionException` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `MessageEncryptionException` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `SignatureVerificationException` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/SignatureVerificationException.kt` |
| `UnsupportedCryptoVersionException` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `GroupCiphertext` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/GroupCiphertext.kt` |
| `GroupCrypto` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/GroupCrypto.kt` |
| `GroupKeyConfirmation` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/GroupKeyConfirmation.kt` |
| `GroupKeyStore` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/GroupKeyStore.kt` |
| `SodiumGroupCrypto` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/SodiumGroupCrypto.kt` |
| `CryptoHash` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/hash/CryptoHash.kt` |
| `DefaultCryptoHash` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/hash/DefaultCryptoHash.kt` |
| `IdentityAcknowledgementCrypto` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/IdentityAcknowledgementCrypto.kt` |
| `IdentityAcknowledgementPayloadEncoder` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/IdentityAcknowledgementPayloadEncoder.kt` |
| `IdentityKeyGenerator` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/IdentityKeyGenerator.kt` |
| `IdentityKeyPair` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/IdentityKeyPair.kt` |
| `SodiumIdentityAcknowledgementCrypto` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/SodiumIdentityAcknowledgementCrypto.kt` |
| `SodiumIdentityKeyGenerator` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/SodiumIdentityKeyGenerator.kt` |
| `PublicIdentityKeySet` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/model/PublicIdentityKeySet.kt` |
| `SecureRandomGenerator` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/random/SecureRandomGenerator.kt` |
| `SodiumSecureRandomGenerator` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/random/SodiumSecureRandomGenerator.kt` |
| `SafetyNumber` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/safety/SafteyNumber.kt` |
| `SafetyNumberGenerator` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/safety/SafetyNumberGenerator.kt` |
| `DetachedSignatureCrypto` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/signature/DetachedSignatureCrypto.kt` |
| `SodiumDetachedSignatureCrypto` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/signature/SodiumDetachedSignatureCrypto.kt` |
| `DecodedTransportMessage` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DecodedTransportMessage.kt` |
| `DefaultIncomingTransportMessageDecoder` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DefaultIncomingTransportMessageDecoder.kt` |
| `DefaultTransportPayloadCodec` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DefaultTransportPayloadCodec.kt` |
| `EncryptedTransportPayload` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/EncryptedTransportPayload.kt` |
| `IncomingTransportMessageDecoder` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/IncomingTransportMessageDecoder.kt` |
| `SodiumTransportMessageCipher` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/SodiumTransportMessageCipher.kt` |
| `TransportEncryptionMode` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/TransportEncryptionMode.kt` |
| `TransportMessageCipher` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/TransportMessageCipher.kt` |
| `TransportPayloadCodec` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/TransportPayloadCodec.kt` |
| `ByteArrays` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/util/ByteArrays.kt` |

## `:core:protocol`

Path: `core/protocol`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:crypto`

Production top-level declarations: **109**

| Class/type | Source set | File |
|---|---|---|
| `EncryptedBlobReference` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/MessageAttachment.kt` |
| `GroupPinnedAttachmentProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/GroupPinnedAttachmentProvider.kt` |
| `MessageAttachment` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/MessageAttachment.kt` |
| `MessageAttachmentConstraints` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/MessageAttachmentConstraints.kt` |
| `MessageAttachmentType` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/MessageAttachment.kt` |
| `DirectChatAuthorizationRevocationProtocol` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/authorization/DirectChatAuthorizationRevocationProtocol.kt` |
| `DirectChatAuthorizationRevocationSender` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/authorization/DirectChatAuthorizationRevocationSender.kt` |
| `GroupAvatarMetadata` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/avatar/GroupAvatarMetadata.kt` |
| `GroupAvatarPayload` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/avatar/GroupAvatarMetadata.kt` |
| `GroupAvatarProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/avatar/GroupAvatarProvider.kt` |
| `GroupAvatarSnapshot` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/avatar/GroupAvatarProvider.kt` |
| `KotlinxPacketCodec` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/codec/KotlinxPacketCodec.kt` |
| `PacketCodec` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/codec/PacketCodec.kt` |
| `DefaultProtocolPacketHandler` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/DefaultProtocolPacketHandler.kt` |
| `IncomingMessageHandler` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/IncomingMessageHandler.kt` |
| `IncomingMessageRejectedException` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/IncomingMessageRejectedException.kt` |
| `IncomingPacketContext` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/IncomingPacketContext.kt` |
| `ProtocolPacketHandler` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/ProtocolPacketHandler.kt` |
| `TypedProtocolPacketHandler` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/TypeProtocolHandler.kt` |
| `LocalEncryptionKeyPair` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalEncryptionKeyPair.kt` |
| `LocalEncryptionKeyPairProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalEncryptionKeyPairProvider.kt` |
| `LocalIdentityChangeHandler` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalIdentityChangeHandler.kt` |
| `LocalIdentityUnavailableException` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalIdentityUnavailableException.kt` |
| `LocalPublicIdentity` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalPublicIdentity.kt` |
| `LocalPublicIdentityProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalPublicIdentityProvider.kt` |
| `LocalSigningKeyPair` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalSigningKeyPair.kt` |
| `LocalSigningKeyPairProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalSigningKeyPairProvider.kt` |
| `LocalSigningPublicKeyProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalSigningPublicKeyProvider.kt` |
| `ContactInvitationDeclineProtocol` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/invitation/ContactInvitationDeclineProtocol.kt` |
| `ContactInvitationHandshakeProtocol` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/invitation/ContactInvitationHandshakeProtocol.kt` |
| `ContactInvitationPayloadEncoder` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/invitation/ContactInvitationPayloadEncoder.kt` |
| `ContactInvitationRequestProtocol` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/invitation/ContactInvitationRequestProtocol.kt` |
| `LocalMailboxCredential` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `MailboxCapabilityLifecycle` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `MailboxDeliveryRoute` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `MailboxRouteRepository` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `NoOpMailboxCapabilityLifecycle` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `GroupMessageContent` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/GroupMessageContent.kt` |
| `GroupMessageContentCodec` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/GroupMessageContent.kt` |
| `MessageDeletionPayload` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageDeletionPayload.kt` |
| `MessageDeletionPayloadCodec` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageDeletionPayloadCodec.kt` |
| `MessageEditPayload` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageEditPayload.kt` |
| `MessageEditPayloadCodec` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageEditPayloadCodec.kt` |
| `MessageReactionPayload` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageReactionPayload.kt` |
| `OutboxDeliveryStateListener` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxDeliveryStateListener.kt` |
| `OutboxEvent` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxEvent.kt` |
| `OutboxProcessingResult` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxProcessor.kt` |
| `OutboxProcessor` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxProcessor.kt` |
| `OutboxRunner` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxRunner.kt` |
| `OutboxStateMachine` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxStateMachine.kt` |
| `OutboxStatus` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxStatus.kt` |
| `ProtocolOutbox` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/ProtocolOutbox.kt` |
| `ProtocolOutboxFailureEvent` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/ProtocolOutboxFailureEvent.kt` |
| `ProtocolOutboxItem` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/ProtocolOutboxItem.kt` |
| `ChatMessagePacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ChatMessagePacket.kt` |
| `ContactInviteAcceptedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `ContactInviteDeclinedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `ContactInvitePacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `ContactReadyPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `ContactVerificationReceiptPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `DeliveryReceiptPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/DeliveryReceiptPacket.kt` |
| `DirectChatAuthorizationRevokedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `GroupAvatarUpdatedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupAvatarUpdatedPacket.kt` |
| `GroupChatMessagePacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupChatMessagePacket.kt` |
| `GroupConversationDeletedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupConversationDeletedPacket.kt` |
| `GroupCreatedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupCreatedPacket.kt` |
| `GroupDescriptionUpdatedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupDescriptionUpdatedPacket.kt` |
| `GroupInviteDeclinedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupInviteDeclinedPacket.kt` |
| `GroupInvitePacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupInvitePacket.kt` |
| `GroupInviteReceivedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupInviteReceivedPacket.kt` |
| `GroupJoinRequestPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupJoinRequestPacket.kt` |
| `GroupLeaveRequestPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupLeaveRequestPacket.kt` |
| `GroupMemberActivatedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMemberActivatedPacket.kt` |
| `GroupMemberActivationAcknowledgementPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMemberActivationAcknowledgementPacket.kt` |
| `GroupMemberPayload` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMemberPayload.kt` |
| `GroupMemberRemovedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMemberRemovedPacket.kt` |
| `GroupMembershipChangePayload` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupCreatedPacket.kt` |
| `GroupMessageDeletionPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMessageDeletionPacket.kt` |
| `GroupMessageEditPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMessageEditPacket.kt` |
| `GroupPinUpdatedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupPinUpdatedPacket.kt` |
| `GroupProtocolPayloadEncoder` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupProtocolPayloadEncoder.kt` |
| `GroupReadyAcknowledgementPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupReadyAcknowledgementPacket.kt` |
| `GroupTitleUpdatedPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupTitleUpdatedPacket.kt` |
| `GroupVerificationMemberPayload` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupVerificationPackets.kt` |
| `GroupVerificationReceiptPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupVerificationPackets.kt` |
| `GroupVerificationSnapshotPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupVerificationPackets.kt` |
| `GroupVerificationSnapshotRequestPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupVerificationPackets.kt` |
| `IdentityAcknowledgementPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/IdentityAcknowledgementPacket.kt` |
| `IdentityPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/IdentityPacket.kt` |
| `MailboxRoutePacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/MailboxRoutePacket.kt` |
| `MessageDeletionPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/MessageDeletionPacket.kt` |
| `MessageEditPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/MessageEditPacket.kt` |
| `ReadReceiptPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ReadReceiptPacket.kt` |
| `SparrowPacket` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/SparrowPacket.kt` |
| `DefaultPhoneNumberNormalizer` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/phone/DefaultPhoneNumberNormalizer.kt` |
| `LocalPhoneNumberProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/phone/LocalPhoneNumberProvider.kt` |
| `PhoneNumberNormalizer` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/phone/PhoneNumberNormalizer.kt` |
| `LocalProfilePictureMetadataProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/LocalProfilePictureMetadataProvider.kt` |
| `LocalProfilePictureProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/LocalProfilePictureProvider.kt` |
| `LocalProfilePictureSnapshot` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/LocalProfilePictureProvider.kt` |
| `ProfilePictureMetadata` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/ProfilePictureMetadata.kt` |
| `ProfilePicturePayload` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/ProfilePictureMetadata.kt` |
| `RemoteProfilePictureMetadataProcessor` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/RemoteProfilePictureMetadataProcessor.kt` |
| `RemoteProfilePictureProvider` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/RemoteProfilePictureProvider.kt` |
| `RemoteProfilePictureSnapshot` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/RemoteProfilePictureProvider.kt` |
| `ByteArrayAsBase64Serializer` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/serializer/ByteArrayAsBase64Serializer.kt` |
| `OutgoingWireAcceptance` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/transport/OutgoingWireSender.kt` |
| `OutgoingWireSender` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/transport/OutgoingWireSender.kt` |
| `ProtocolVersion` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/version/ProtocolVersion.kt` |

## `:feature:messaging`

Path: `feature/messaging`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:protocol`

Production top-level declarations: **21**

| Class/type | Source set | File |
|---|---|---|
| `MessagingFailureEvent` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingFailureEvent.kt` |
| `MessagingIndicator` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingIndicator.kt` |
| `MessagingTransportResult` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingTransportResult.kt` |
| `MessagingTransportState` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingTransportResult.kt` |
| `AcknowledgeMessagingFailureUseCase` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/AcknowledgeMessagingFailureUseCase.kt` |
| `ObserveMessagingFailureEventsUseCase` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingFailureEventsUseCase.kt` |
| `ObserveMessagingIndicatorsUseCase` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingIndicatorsUseCase.kt` |
| `ObserveMessagingTransportResultsUseCase` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingTransportResultsUseCase.kt` |
| `SendEncodedTransportUseCase` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/SendEncodedTransportUseCase.kt` |
| `SendMessagingIndicatorUseCase` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/SendMessagingIndicatorUseCase.kt` |
| `DefaultIncomingEnvelopeRunner` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/DefaultIncomingEnvelopeRunner.kt` |
| `IncomingEnvelopeGateway` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeGateway.kt` |
| `IncomingEnvelopeProcessingResult` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeProcessor.kt` |
| `IncomingEnvelopeProcessor` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeProcessor.kt` |
| `IncomingEnvelopeRunner` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeRunner.kt` |
| `IncomingTransportEnvelope` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeGateway.kt` |
| `MessagingIndicatorGateway` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/indicator/MessagingIndicatorGateway.kt` |
| `MailboxCoordinator` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/mailbox/MailboxCoordinator.kt` |
| `MailboxRoutePayloadEncoder` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/mailbox/MailboxRoutePayloadEncoder.kt` |
| `DefaultOutboxProcessor` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxProcessor.kt` |
| `DefaultOutboxRunner` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxRunner.kt` |

## `:feature:transport`

Path: `feature/transport`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:data:datastore`, `:core:crypto`, `:core:protocol`

Production top-level declarations: **89**

| Class/type | Source set | File |
|---|---|---|
| `TransportConfig` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/config/TransportConfig.kt` |
| `DefaultTransportConnectionManager` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/DefaultTransportConnectionManager.kt` |
| `TransportConnectionManager` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionManager.kt` |
| `TransportConnectionState` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionState.kt` |
| `TransportDiagnosticsState` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportDiagnosticsState.kt` |
| `ControlPlaneCandidateVerifier` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneCandidateVerifier.kt` |
| `ControlPlaneConfigurationImpl` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneConfigurationImpl.kt` |
| `ControlPlaneDirectoryRemoteSource` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneDirectoryRemoteSource.kt` |
| `ControlPlaneRequestRejectedException` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestExceptions.kt` |
| `ControlPlaneRequestRouter` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestRouter.kt` |
| `ControlPlaneUnavailableException` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestExceptions.kt` |
| `DirectoryEnvelope` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `DirectoryPayload` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `DirectoryPlane` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `HttpControlPlaneDirectorySynchronizer` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/HttpControlPlaneDirectorySynchronizer.kt` |
| `HttpControlPlaneHealthMonitor` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/HttpControlPlaneHealthMonitor.kt` |
| `HttpNodeControlPlaneDirectorySource` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDirectorySource.kt` |
| `NodeControlPlaneDirectory` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDirectorySource.kt` |
| `NodeControlPlaneDirectorySource` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDirectorySource.kt` |
| `NodeControlPlaneDiscoverySynchronizer` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDiscoverySynchronizer.kt` |
| `SignedControlPlaneDirectoryVerifier` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `SignedDirectoryControlPlaneCandidateVerifier` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneCandidateVerifier.kt` |
| `SignedDirectoryDownload` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneDirectoryRemoteSource.kt` |
| `VerifiedControlPlaneDirectoryState` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/VerifiedControlPlaneDirectoryState.kt` |
| `CachedNodeDirectory` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryCache.kt` |
| `DataStoreNodeDirectoryCache` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/DataStoreNodeDirectoryCache.kt` |
| `DefaultNodeEndpointResolver` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/DefaultNodeEndpointResolver.kt` |
| `FailedNodeTracker` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/FailedNodeTracker.kt` |
| `HttpNodeDirectorySource` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectorySource.kt` |
| `NodeCapability` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `NodeDirectory` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `NodeDirectoryCache` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryCache.kt` |
| `NodeDirectorySource` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectorySource.kt` |
| `NodeDirectoryVerifier` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryVerifier.kt` |
| `NodeEndpoint` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `NodeEndpointResolver` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeEndpointResolver.kt` |
| `NodeEndpointSelector` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeEndpointSelector.kt` |
| `RegistryAuthorityCertificate` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `RegistrySigningCertificate` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `SignedNodeDirectory` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `SparrowNodeDescriptor` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `UnsignedNodeDescriptor` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `UnsignedRegistryAuthorityCertificate` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `UnsignedRegistrySigningCertificate` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `ClientRoute` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `ClientRouteRegistration` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `FederatedEnvelope` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/FederatedEnvelope.kt` |
| `GatewayBlobUploadTicket` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayBlobUploadTicket.kt` |
| `GatewayBlobUploadTicketRequest` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayBlobUploadTicket.kt` |
| `GatewayClientMessage` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `GatewayEnvelopeAcceptance` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayEnvelopeAcceptance.kt` |
| `GatewayIndicatorEvent` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayIndicatorEvent.kt` |
| `GatewayNodeInformation` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `GatewayServerMessage` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `TransportEnvelope` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/TransportEnvelope.kt` |
| `UnsignedClientRoute` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `CreateMailboxRequest` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `CreateMailboxResponse` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `HttpMailboxGateway` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/HttpMailboxGateway.kt` |
| `MailboxEnvelopesResponse` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `MailboxGateway` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `ClientPresenceRouteCoordinator` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `ClientPresenceRouteEffect` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteEvent` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteSession` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `ClientPresenceRouteState` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteStateMachine` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteTransition` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientRouteRegistrationFactory` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientRouteRegistrationFactory.kt` |
| `PresenceRouteConnection` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `PresenceRouteRefreshRejectedException` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/PresenceRouteRefreshRejectedException.kt` |
| `HttpPushTokenRegistrationGateway` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/HttpPushTokenRegistrationGateway.kt` |
| `PushDeviceRegistrationRequest` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/PushModels.kt` |
| `PushPlatform` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/PushTokenRegistrationGateway.kt` |
| `PushTokenRegistrationGateway` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/PushTokenRegistrationGateway.kt` |
| `HttpPendingEnvelopeGateway` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/inbox/HttpPendingEnvelopeGateway.kt` |
| `PendingEnvelopeGateway` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/inbox/PendingEnvelopeGateway.kt` |
| `PendingTransportEnvelopesResponse` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/inbox/PendingEnvelopeModels.kt` |
| `DefaultLocalBootstrapRoutingIdProvider` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/DefaultLocalBootstrapRoutingIdProvider.kt` |
| `DefaultLocalRoutingIdProvider` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/DefaultLocalRoutingIdProvider.kt` |
| `LocalBootstrapRoutingIdProvider` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/LocalBootstrapRoutingIdProvider.kt` |
| `LocalRoutingIdProvider` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/LocalRoutingIdProvider.kt` |
| `RoutingIdGenerator` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/RoutingIdGenerator.kt` |
| `Sha256RoutingIdGenerator` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/Sha256RoutingIdGenerator.kt` |
| `WebSocketOutgoingWireSender` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/sender/WebSocketOutgoingWireSender.kt` |
| `DefaultWebSocketTransportClient` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/DefaultWebSocketTransportClient.kt` |
| `GatewayPendingRequestRegistry` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/GatewayPendingRequestRegistry.kt` |
| `GatewayServerMessageHandler` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/GatewayServerMessageHandler.kt` |
| `WebSocketTransportClient` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/WebSocketTransportClient.kt` |

## `:feature:onboarding`

Path: `feature/onboarding`  
Direct project dependencies detected from `build.gradle.kts`: `:core:ui`, `:feature:identity`, `:feature:media`

Production top-level declarations: **7**

| Class/type | Source set | File |
|---|---|---|
| `AutomaticPhoneNumberResult` | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/device/OnboardingPermissions.kt` |
| `PermissionRequestResult` | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/device/OnboardingPermissions.kt` |
| `OnboardingViewModel` | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/OnboardingViewModel.kt` |
| `OnboardingPage` | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiState.kt` |
| `OnboardingUiEvent` | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiEvent.kt` |
| `OnboardingUiState` | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiState.kt` |
| `PageData` | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/pages/PermissionsPage.kt` |

## `:startup`

Path: `startup`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:ui`, `:core:embedding`, `:feature:identity`, `:feature:onboarding`, `:feature:search`, `:feature:safety`, `:feature:transport`

Production top-level declarations: **9**

| Class/type | Source set | File |
|---|---|---|
| `AppConnectionAvailability` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/model/AppConnectionAvailability.kt` |
| `ObserveAppConnectionAvailabilityUseCase` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/usecase/ObserveAppConnectionAvailabilityUseCase.kt` |
| `StartupViewModel` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/StartupViewModel.kt` |
| `AppInitializationResult` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/AppInitializationResult.kt` |
| `StartupConnection` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiState.kt` |
| `StartupUiEvent` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiEvent.kt` |
| `StartupUiState` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiState.kt` |
| `AppInitializer` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/util/AppInitializer.kt` |
| `StartupRuntimeReadiness` | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/util/StartupRuntimeReadiness.kt` |

## `:navigation`

Path: `navigation`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:ui`, `:feature:attachments`, `:feature:autoreply`, `:feature:chats`, `:feature:contactimport`, `:feature:contacts`, `:feature:conversationorchestration`, `:feature:identity`, `:feature:invite`, `:feature:membership`, `:feature:media`, `:feature:onboarding`, `:feature:settings`, `:feature:search`, `:feature:safety`, `:notification`, `:startup`

Production top-level declarations: **5**

| Class/type | Source set | File |
|---|---|---|
| `RecoveryInboxViewModel` | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/presentation/inbox/RecoveryInboxViewModel.kt` |
| `MainViewModel` | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/presentation/main/MainViewModel.kt` |
| `MainTab` | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/presentation/main/model/MainTab.kt` |
| `FeedbackKind` | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/routing/AppNavigation.kt` |
| `FeedbackSnackbarVisuals` | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/routing/AppNavigation.kt` |

## `:notification`

Path: `notification`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:crypto`, `:feature:chats`, `:feature:messaging`, `:feature:transport`, `:core:protocol`, `:resources`

Production top-level declarations: **25**

| Class/type | Source set | File |
|---|---|---|
| `AndroidNotificationRuntime` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/AndroidNotificationRuntime.kt` |
| `BackgroundDeliveryReceiptSender` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/BackgroundDeliveryReceiptSender.kt` |
| `PendingMessageSyncScheduler` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/PendingMessageSyncScheduler.kt` |
| `PendingMessageSyncWorker` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/PendingMessageSyncWorker.kt` |
| `PushTokenRegistrationScheduler` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/PushTokenRegistrationScheduler.kt` |
| `PushTokenRegistrationWorker` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/PushTokenRegistrationWorker.kt` |
| `SparrowDeepLink` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowDeepLink.kt` |
| `SparrowFirebaseMessagingService` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowFirebaseMessagingService.kt` |
| `SparrowNotificationIntentFactory` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowNotificationIntentFactory.kt` |
| `SparrowNotificationIntentHandler` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowNotificationIntentHandler.kt` |
| `SparrowNotificationManager` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowNotificationManager.kt` |
| `PlatformNotificationRuntime` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/device/PlatformNotificationRuntime.kt` |
| `AppVisibilityState` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/AppVisibilityState.kt` |
| `ConversationNotification` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/ConversationNotification.kt` |
| `ConversationNotificationEvent` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/ConversationNotificationEvent.kt` |
| `NotificationConversationTarget` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/NotificationConversationTarget.kt` |
| `PendingMessageSyncResult` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/PendingMessageSyncResult.kt` |
| `ObserveConversationNotificationEventsUseCase` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/usecase/ObserveConversationNotificationEventsUseCase.kt` |
| `RegisterPushTokenUseCase` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/usecase/RegisterPushTokenUseCase.kt` |
| `ResolveNotificationConversationUseCase` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/usecase/ResolveNotificationConversationUseCase.kt` |
| `SynchronizePendingMessagesUseCase` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/usecase/SynchronizePendingMessagesUseCase.kt` |
| `ConversationNotificationCoordinator` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/presentation/ConversationNotificationCoordinator.kt` |
| `ConversationNotificationPresenter` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/presentation/ConversationNotificationPresenter.kt` |
| `NotificationNavigationController` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/presentation/navigation/NotificationNavigationController.kt` |
| `NotificationNavigationTarget` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/presentation/navigation/NotificationNavigationTarget.kt` |

## `:core:ui`

Path: `core/ui`  
Direct project dependencies detected from `build.gradle.kts`: `:resources`, `:core`

Production top-level declarations: **44**

| Class/type | Source set | File |
|---|---|---|
| `BlockScreenshotFlagManager` | `androidMain` | `core/ui/src/androidMain/kotlin/com/cbgm/sparrow/core/ui/component/BlockScreenshotEffect.android.kt` |
| `FeedbackOverlayData` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/AnchoredFeedback.kt` |
| `ParsedSparrowPath` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/StartupArtwork.kt` |
| `PatternElement` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/PatternBackgrund.kt` |
| `SparrowOverlayAnchor` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/SparrowOverlay.kt` |
| `SparrowOverlayScope` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/SparrowOverlay.kt` |
| `SparrowOverlayScopeImpl` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/SparrowOverlay.kt` |
| `SparrowPathSpec` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/StartupArtwork.kt` |
| `SparrowScrollState` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/Scaffold.kt` |
| `SparrowScrollStateType` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/Scaffold.kt` |
| `SparrowTabbedScrollStates` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/Scaffold.kt` |
| `SwipeRevealAction` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/SwipeRevealActions.kt` |
| `ClipboardWriter` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/device/clipboard/ClipboardWriter.kt` |
| `BorderSides` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/helper/Border.kt` |
| `AppLanguage` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/locale/AppLanguage.kt` |
| `AppNavigationEvent` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppNavigationEvent.kt` |
| `AppNavigator` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppNavigator.kt` |
| `AppRoute` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `BarsState` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/scroll/BarsState.kt` |
| `ActionItem` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `Alpha` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `AttachmentColors` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/AttachmentColors.kt` |
| `ButtonSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `CardSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `Colors` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Colors.kt` |
| `ContactDetailsScreenShapes` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `ContactsScreenShapes` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `ContactsScreenSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `Dimens` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `DirectConversationScreenSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `FieldSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `FunctionalColors` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Colors.kt` |
| `GroupConversationScreenSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `GroupDetailsScreenSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `IdentityScreenSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `ImportIdentityScreenSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `MessageBubbleShapes` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `MessageBubbleSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `MessageInputShapes` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `MessageListSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `OverviewScreenSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `ScanIdentityScreenShapes` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `Spacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `StartupScreenSpacing` | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |

## `:feature:settings`

Path: `feature/settings`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:data:datastore`, `:core:embedding`, `:core:ui`, `:feature:identity`, `:feature:contacts`, `:feature:avatar`, `:feature:autoreply`, `:feature:voice`, `:feature:search`, `:feature:safety`

Production top-level declarations: **67**

| Class/type | Source set | File |
|---|---|---|
| `AndroidBuildInfoProvider` | `androidMain` | `feature/settings/src/androidMain/kotlin/com/cbgm/sparrow/feature/settings/device/AndroidBuildInfoProvider.kt` |
| `AndroidSystemLanguageProvider` | `androidMain` | `feature/settings/src/androidMain/kotlin/com/cbgm/sparrow/feature/settings/device/AndroidSystemLanguageProvider.kt` |
| `DeveloperErrorLogStorageDataSource` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/DeveloperErrorLogStorageDataSource.kt` |
| `InMemorySettingsStorage` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/InMemorySettingsStorage.kt` |
| `SettingsStorage` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/SettingsStorage.kt` |
| `SettingsStorageImpl` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/SettingsStorageImpl.kt` |
| `DeveloperErrorDto` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/model/DeveloperErrorDto.kt` |
| `ContactBlocklistRepositoryImpl` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/ContactBlocklistRepositoryImpl.kt` |
| `DeveloperErrorLogRepositoryImpl` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/DeveloperErrorLogRepositoryImpl.kt` |
| `DirectIdentitySetupModeRepositoryImpl` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/DirectIdentitySetupModeRepositoryImpl.kt` |
| `LicencesRepositoryImpl` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/LicencesRepositoryImpl.kt` |
| `SettingsRepositoryImpl` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/SettingsRepositoryImpl.kt` |
| `BuildInfoProvider` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/device/BuildInfoProvider.kt` |
| `SystemLanguageProvider` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/device/SystemLanguageProvider.kt` |
| `BuildInfo` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/BuildInfo.kt` |
| `ControlPlaneSettingsContext` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/ControlPlaneSettingsContext.kt` |
| `DeveloperError` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/DeveloperError.kt` |
| `DisclaimerContent` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/DisclaimerContent.kt` |
| `SettingsDomainContext` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/SettingsDomainContext.kt` |
| `DeveloperErrorLogRepository` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/repository/DeveloperErrorLogRepository.kt` |
| `LicensesRepository` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/repository/LicensesRepository.kt` |
| `SettingsRepository` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/repository/SettingsRepository.kt` |
| `ClearDeveloperErrorsUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ClearDeveloperErrorsUseCase.kt` |
| `ClearLocalDataUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ClearLocalDataUseCase.kt` |
| `GetAppLanguageUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetAppLanguageUseCase.kt` |
| `GetBuildInfoUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetBuildInfoUseCase.kt` |
| `GetDeveloperEnabledUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetDeveloperEnabledUseCase.kt` |
| `GetLicensesUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetLicensesUseCase.kt` |
| `InitAppLanguageUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/InitAppLanguageUseCase.kt` |
| `ObserveBlockUnknownContactInvitesUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveBlockUnknownContactInvitesUseCase.kt` |
| `ObserveBlockedContactIdsUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveBlockedContactIdsUseCase.kt` |
| `ObserveControlPlaneSettingsContextUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveControlPlaneSettingsContextUseCase.kt` |
| `ObserveDeveloperErrorsUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveDeveloperErrorsUseCase.kt` |
| `ObserveDirectIdentitySetupModeUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveDirectIdentitySetupModeUseCase.kt` |
| `ObserveSettingsDomainContextUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveSettingsDomainContextUseCase.kt` |
| `SetAppLanguageUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetAppLanguageUseCase.kt` |
| `SetBlockUnknownContactInvitesUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetBlockUnknownContactInvitesUseCase.kt` |
| `SetDeveloperEnabledUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetDeveloperEnabledUseCase.kt` |
| `SetDirectIdentitySetupModeUseCase` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetDirectIdentitySetupModeUseCase.kt` |
| `DeveloperMenuViewModel` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/DeveloperMenuViewModel.kt` |
| `NodeCooldown` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/components/NetworkDiagnosticsCard.kt` |
| `DeveloperMenuUiEvent` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiEvent.kt` |
| `DeveloperMenuUiState` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiState.kt` |
| `DeveloperNodesViewModel` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/nodes/DeveloperNodesViewModel.kt` |
| `DisclaimerViewModel` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/DisclaimerViewModel.kt` |
| `DisclaimerType` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/model/DisclaimerType.kt` |
| `DisclaimerUiEvent` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/model/DisclaimerUiEvent.kt` |
| `DeveloperErrorLogViewModel` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/DeveloperErrorLogViewModel.kt` |
| `DeveloperErrorLogUiEvent` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiEvent.kt` |
| `DeveloperErrorLogUiState` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiState.kt` |
| `DeveloperErrorUi` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorUi.kt` |
| `LicensesViewModel` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/LicensesViewModel.kt` |
| `LicensesUiEvent` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/model/LicensesUiEvent.kt` |
| `LicensesUiState` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/model/LicensesUiState.kt` |
| `ControlPlaneSettingsViewModel` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/ControlPlaneSettingsViewModel.kt` |
| `ControlPlaneAddSource` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneDirectoryError` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneSettingsError` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneSettingsUiEvent` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneSettingsUiState` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneUiModel` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneUiSource` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneUiStatus` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `SettingsViewModel` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/SettingsViewModel.kt` |
| `SettingsEffect` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsEffect.kt` |
| `SettingsUiEvent` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `SettingsUiState` | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiState.kt` |

## `:feature:search`

Path: `feature/search`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:embedding`, `:core:ui`, `:data:database`

Production top-level declarations: **22**

| Class/type | Source set | File |
|---|---|---|
| `MessageSearchIndexDataSource` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/datasource/MessageSearchIndexDataSource.kt` |
| `MessageSearchLocalDataSource` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/datasource/MessageSearchLocalDataSource.kt` |
| `SemanticSearchEmbeddingDataSource` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/datasource/SemanticSearchEmbeddingDataSource.kt` |
| `EmbeddingCodecMapper` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/mapper/EmbeddingCodecMapper.kt` |
| `SemanticSearchIndexConfig` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/model/SemanticSearchIndexConfig.kt` |
| `MessageSearchRepositoryImpl` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/repository/MessageSearchRepositoryImpl.kt` |
| `SemanticSearchRepositoryImpl` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/repository/SemanticSearchRepositoryImpl.kt` |
| `MessageSearchConversationType` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/MessageSearchConversationType.kt` |
| `MessageSearchMatchType` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/MessageSearchMatchType.kt` |
| `MessageSearchResult` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/MessageSearchResult.kt` |
| `SemanticSearchState` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/SemanticSearchState.kt` |
| `MessageSearchRepository` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/repository/MessageSearchRepository.kt` |
| `SemanticSearchRepository` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/repository/SemanticSearchRepository.kt` |
| `InitializeSemanticSearchUseCase` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/InitializeSemanticSearchUseCase.kt` |
| `ObserveSemanticSearchStateUseCase` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/ObserveSemanticSearchStateUseCase.kt` |
| `SearchMessagesUseCase` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/SearchMessagesUseCase.kt` |
| `SetSemanticSearchEnabledUseCase` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/SetSemanticSearchEnabledUseCase.kt` |
| `MessageSearchViewModel` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/MessageSearchViewModel.kt` |
| `MessageSearchMode` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchMode.kt` |
| `MessageSearchResultUi` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchResultUi.kt` |
| `MessageSearchUiEvent` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiEvent.kt` |
| `MessageSearchUiState` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiState.kt` |

## `:feature:safety`

Path: `feature/safety`  
Direct project dependencies detected from `build.gradle.kts`: `:core`, `:core:embedding`, `:core:ui`, `:data:database`, `:feature:contacts`

Production top-level declarations: **24**

| Class/type | Source set | File |
|---|---|---|
| `MessageSafetyEmbeddingDataSource` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/datasource/MessageSafetyEmbeddingDataSource.kt` |
| `MessageSafetyLocalDataSource` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/datasource/MessageSafetyLocalDataSource.kt` |
| `GeneratedMessageSafetyMlpModel` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/model/GeneratedMessageSafetyMlpModel.kt` |
| `MessageSafetyAnalysisRepositoryImpl` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/repository/MessageSafetyAnalysisRepositoryImpl.kt` |
| `MessageSafetyRepositoryImpl` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/repository/MessageSafetyRepositoryImpl.kt` |
| `MessageSafetyAssessment` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyAssessment.kt` |
| `MessageSafetyCandidate` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyCandidate.kt` |
| `MessageSafetyReason` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyReason.kt` |
| `MessageSafetyState` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyState.kt` |
| `MessageSafetyAnalysisRepository` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/repository/MessageSafetyAnalysisRepository.kt` |
| `MessageSafetyRepository` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/repository/MessageSafetyRepository.kt` |
| `AnalyzeMessageSafetyUseCase` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/AnalyzeMessageSafetyUseCase.kt` |
| `InitializeMessageSafetyUseCase` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/InitializeMessageSafetyUseCase.kt` |
| `ObserveMessageSafetyAssessmentsUseCase` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/ObserveMessageSafetyAssessmentsUseCase.kt` |
| `ObserveMessageSafetyStateUseCase` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/ObserveMessageSafetyStateUseCase.kt` |
| `ProcessMessageSafetyBatchUseCase` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/ProcessMessageSafetyBatchUseCase.kt` |
| `MessageSafetyDetailsViewModel` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/MessageSafetyDetailsViewModel.kt` |
| `MessageSafetyDetailsUiEvent` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyDetailsUiEvent.kt` |
| `MessageSafetyDetailsUiState` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyDetailsUiState.kt` |
| `MessageSafetyWarningLevel` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyWarningLevel.kt` |
| `MessageSafetyWarningReason` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyWarningReason.kt` |
| `MessageSafetyWarningUi` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyWarningUi.kt` |
| `MessageSafetyClassifier` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/util/MessageSafetyClassifier.kt` |
| `MessageSafetyStructuralAnalyzer` | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/util/MessageSafetyStructuralAnalyzer.kt` |

## `:quality:detekt-rules`

Path: `quality/detekt-rules`  
Direct project dependencies detected from `build.gradle.kts`: none

Production top-level declarations: **10**

| Class/type | Source set | File |
|---|---|---|
| `SparrowRuleSetProvider` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/SparrowRuleSetProvider.kt` |
| `DaoUsageRule` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/DaoUsageRule.kt` |
| `LayerDependencyRule` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/LayerDependencyRule.kt` |
| `NoNotNullAssertionRule` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/NoNotNullAssertionRule.kt` |
| `NoPlatformImportInCommonMainRule` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/NoPlatformImportInCommonMainRule.kt` |
| `NoTestImportInProductionRule` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/NoTestImportInProductionRule.kt` |
| `RepositoryDependencyRule` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/RepositoryDependencyRule.kt` |
| `UseCaseDependencyRule` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/UseCaseDependencyRule.kt` |
| `ViewModelDirectDataDependencyRule` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/ViewModelDirectDataDependencyRule.kt` |
| `WeakHashAlgorithmRule` | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/WeakHashAlgorithmRule.kt` |

## `:resources`

Path: `resources`  
Direct project dependencies detected from `build.gradle.kts`: none

Production top-level declarations: **0**

## `:server:protocol`

Path: `server/protocol`  
Direct project dependencies detected from `build.gradle.kts`: none

Production top-level declarations: **35**

| Class/type | Source set | File |
|---|---|---|
| `BlobMetadata` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/BlobModels.kt` |
| `BlobUploadTicketClaims` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/BlobModels.kt` |
| `ClientRoute` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `ClientRouteRegistration` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `ClientRoutingResult` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `CreateMailboxRequest` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `CreateMailboxResponse` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `DeliveryRoute` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `EnvelopeAcceptanceState` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `ErrorResponse` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `FederatedEnvelope` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `FederatedIndicatorEvent` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `FederationAcknowledgement` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `GatewayClientMessage` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `GatewayLoad` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `GatewayNodeInformation` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `GatewayServerMessage` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `MailboxEnvelopeRequest` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `MailboxEnvelopesResponse` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `NodeCapability` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `NodeDirectory` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `NodeHeartbeatRequest` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `NodeRegistrationRequest` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `PendingTransportEnvelopesResponse` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `PushDeviceRegistrationRequest` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `RegistryAuthorityCertificate` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `RegistrySigningCertificate` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `SignedNodeDirectory` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `SparrowNodeDescriptor` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `TransportEnvelope` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `UnsignedClientRoute` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `UnsignedNodeDescriptor` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `UnsignedNodeHeartbeat` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `UnsignedRegistryAuthorityCertificate` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `UnsignedRegistrySigningCertificate` | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |

## `:server:security`

Path: `server/security`  
Direct project dependencies detected from `build.gradle.kts`: `:server:protocol`

Production top-level declarations: **22**

| Class/type | Source set | File |
|---|---|---|
| `BoundedRateLimiter` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `ClientRateLimitKeys` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `ClientRoutingIds` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/ClientRoutingIds.kt` |
| `CommandLineOutput` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/CommandLineOutput.kt` |
| `InternalApiAuthentication` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/InternalApiAuthentication.kt` |
| `NodeIdentity` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeIdentity.kt` |
| `NodeIdentityStore` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeIdentityStore.kt` |
| `NodeIds` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeIds.kt` |
| `NodeRequestAuthentication` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeRequestAuthorizationRequirements` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthorizer.kt` |
| `NodeRequestAuthorizer` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthorizer.kt` |
| `NodeRequestHeaders` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeRequestSignatureCli` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestSignatureCli.kt` |
| `NodeRequestSigner` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeRequestVerifier` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `PresenceRouteRegistrationCli` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/PresenceRouteRegistrationCli.kt` |
| `ProtocolSignatures` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/ProtocolSignatures.kt` |
| `RateLimitDecision` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `RateLimitPolicy` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `RegistryCertificateSignatures` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RegistryCertificateSignatures.kt` |
| `ReplayProtection` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/ReplayProtection.kt` |
| `Signatures` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/Signatures.kt` |

## `:server:persistence`

Path: `server/persistence`  
Direct project dependencies detected from `build.gradle.kts`: `:server:protocol`

Production top-level declarations: **3**

| Class/type | Source set | File |
|---|---|---|
| `BoundedIdempotencyStore` | `main` | `server/persistence/src/main/kotlin/com/cbgm/sparrow/server/persistence/BoundedIdempotencyStore.kt` |
| `ControlPlaneEndpointPool` | `main` | `server/persistence/src/main/kotlin/com/cbgm/sparrow/server/persistence/ServiceEnvironment.kt` |
| `ServiceEnvironment` | `main` | `server/persistence/src/main/kotlin/com/cbgm/sparrow/server/persistence/ServiceEnvironment.kt` |

## `:server:observability`

Path: `server/observability`  
Direct project dependencies detected from `build.gradle.kts`: none

Production top-level declarations: **1**

| Class/type | Source set | File |
|---|---|---|
| `ReadinessProbe` | `main` | `server/observability/src/main/kotlin/com/cbgm/sparrow/server/observability/ReadinessProbe.kt` |

## `:server:node-registry`

Path: `server/node-registry`  
Direct project dependencies detected from `build.gradle.kts`: `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability`

Production top-level declarations: **15**

| Class/type | Source set | File |
|---|---|---|
| `CertifiedRegistrySigner` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `DirectRegistryDirectorySigner` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `NodeRegistryConfig` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/Application.kt` |
| `NodeRegistryStorage` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `NodeRegistryStore` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `PostgresNodeRegistryDatabase` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryDatabase.kt` |
| `PostgresNodeRegistryDatabaseConfig` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryDatabase.kt` |
| `PostgresNodeRegistryStore` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryStore.kt` |
| `RegistrationResult` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `RegistryAuthorityCertificateStore` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryAuthorityCertificateStore.kt` |
| `RegistryAuthorityProvisioningCli` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryAuthorityProvisioningCli.kt` |
| `RegistryDirectorySigner` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `RegistrySigningConfig` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `RegistrySigningRuntime` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistrySigningRuntime.kt` |
| `RotatingRegistryDirectorySigner` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |

## `:server:presence-directory`

Path: `server/presence-directory`  
Direct project dependencies detected from `build.gradle.kts`: `:server:persistence`, `:server:protocol`, `:server:security`, `:server:observability`

Production top-level declarations: **7**

| Class/type | Source set | File |
|---|---|---|
| `PresenceConfig` | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/Application.kt` |
| `PresenceResult` | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceStore.kt` |
| `PresenceRouteKey` | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceRoutes.kt` |
| `PresenceRuntime` | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceRuntime.kt` |
| `PresenceStorage` | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceStore.kt` |
| `PresenceStore` | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceStore.kt` |
| `RedisPresenceStore` | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/RedisPresenceStore.kt` |

## `:server:gateway`

Path: `server/gateway`  
Direct project dependencies detected from `build.gradle.kts`: `:server:persistence`, `:server:protocol`, `:server:security`, `:server:observability`, `:server:linkPreview`

Production top-level declarations: **32**

| Class/type | Source set | File |
|---|---|---|
| `BestEffortPresenceClient` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayBackgroundClients.kt` |
| `BlobAlreadyExistsException` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobCleanupAgent` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobCleanupAgent.kt` |
| `BlobStorageCapacityExceededException` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobStore` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobTooLargeException` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobUploadPermitCleanupAgent` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobUploadPermitCleanupAgent.kt` |
| `BlobUploadPermitStore` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobUploadPermitStore.kt` |
| `ConnectionRegistry` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/ConnectionRegistry.kt` |
| `EnvelopeFallbackActions` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayWebSocketHandler.kt` |
| `FederationClient` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |
| `GatewayBlobUploadTicketIssuer` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayBlobUploadTicketIssuer.kt` |
| `GatewayConfig` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/Application.kt` |
| `GatewayConnection` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/ConnectionRegistry.kt` |
| `GatewayControlPlaneDirectory` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayControlPlaneDiscovery.kt` |
| `GatewayControlPlaneDiscovery` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayControlPlaneDiscovery.kt` |
| `GatewayMessageActions` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewayPushActions` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewayPushDispatcher` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayBackgroundClients.kt` |
| `GatewayRouteValidationFailure` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayRouteValidator.kt` |
| `GatewayRouteValidator` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayRouteValidator.kt` |
| `GatewayRuntime` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayRuntime.kt` |
| `GatewaySessionHandler` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewaySessionState` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewaySessionWorkDispatcher` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionWorkDispatcher.kt` |
| `GatewayWebSocketHandler` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayWebSocketHandler.kt` |
| `HttpFederationClient` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `HttpLegacyPushClient` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `HttpNodePushClient` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `HttpPresenceClient` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `LegacyPushClient` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |
| `PresenceClient` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |

## `:server:federation`

Path: `server/federation`  
Direct project dependencies detected from `build.gradle.kts`: `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability`

Production top-level declarations: **21**

| Class/type | Source set | File |
|---|---|---|
| `CachingNodeRegistryClient` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/CachingNodeRegistryClient.kt` |
| `FederationConfig` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/Application.kt` |
| `FederationPeerRouter` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPeerRouter.kt` |
| `FederationRouter` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationRouter.kt` |
| `FederationRuntime` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationRuntime.kt` |
| `HttpGatewayLoadProvider` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `HttpLocalGatewayClient` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `HttpMailboxClient` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `HttpPresenceDirectoryClient` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `HttpRemoteFederationClient` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `ManagedHttpClient` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationRuntime.kt` |
| `NodeRegistrationAgent` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `NodeRegistrationClient` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `NodeRegistrationConfig` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `OutboundEnvelopeEntry` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeQueue.kt` |
| `OutboundEnvelopeQueue` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeQueue.kt` |
| `OutboundEnvelopeRetryAgent` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeRetryAgent.kt` |
| `OutboundEnvelopeStorage` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeQueue.kt` |
| `PostgresOutboundEnvelopeDatabase` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeDatabase.kt` |
| `PostgresOutboundEnvelopeDatabaseConfig` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeDatabase.kt` |
| `PostgresOutboundEnvelopeStorage` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeStorage.kt` |

## `:server:mailbox`

Path: `server/mailbox`  
Direct project dependencies detected from `build.gradle.kts`: `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability`

Production top-level declarations: **14**

| Class/type | Source set | File |
|---|---|---|
| `InMemoryMailbox` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxAuthorization` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxQueries.kt` |
| `MailboxConfig` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/Application.kt` |
| `MailboxCreationResult` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxCredentials` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxStore.kt` |
| `MailboxPushNotifier` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxPushNotifier.kt` |
| `MailboxResult` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxRevocationResult` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxStorage` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxStore` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `PostgresMailboxDatabase` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxDatabase.kt` |
| `PostgresMailboxDatabaseConfig` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxDatabase.kt` |
| `PostgresMailboxQueries` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxQueries.kt` |
| `PostgresMailboxStore` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxStore.kt` |

## `:server:push`

Path: `server/push`  
Direct project dependencies detected from `build.gradle.kts`: `:server:protocol`, `:server:persistence`, `:server:security`, `:server:observability`

Production top-level declarations: **18**

| Class/type | Source set | File |
|---|---|---|
| `FirebasePushSender` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/FirebasePushSender.kt` |
| `InMemoryPendingEnvelopeStore` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `InMemoryPushDeviceStore` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `InMemoryWakeUpStore` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PendingEnvelopeStore` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PostgresPendingEnvelopeStore` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPendingEnvelopeStore.kt` |
| `PostgresPushDatabase` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPushDatabase.kt` |
| `PostgresPushDatabaseConfig` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPushDatabase.kt` |
| `PostgresPushDeviceStore` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPushDeviceStore.kt` |
| `PostgresWakeUpStore` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresWakeUpStore.kt` |
| `PushConfig` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/Application.kt` |
| `PushCoordinator` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushCoordinator.kt` |
| `PushDevice` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PushDeviceStore` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PushNodeApiRuntime` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushRuntime.kt` |
| `PushRuntime` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushRuntime.kt` |
| `PushStores` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `WakeUpStore` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |

## `:server:link-preview`

Path: `server/link-preview`  
Direct project dependencies detected from `build.gradle.kts`: `:server:protocol`

Production top-level declarations: **8**

| Class/type | Source set | File |
|---|---|---|
| `FetchedLinkPreview` | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewModels.kt` |
| `LinkPreviewFetcher` | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewFetcher.kt` |
| `LinkPreviewImage` | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewModels.kt` |
| `LinkPreviewRequest` | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewModels.kt` |
| `LinkPreviewResponse` | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewModels.kt` |
| `LinkPreviewService` | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewService.kt` |
| `LinkPreviewUrlValidator` | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewUrlValidator.kt` |
| `YouTubeMetadata` | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewFetcher.kt` |
