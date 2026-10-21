# Current code inventory

Source-derived declaration inventory of the source tree on **2026-10-21**.

- Kotlin files scanned: **2482**
- Declarations detected: **4539**
- Included Gradle modules: **58**

## Declarations by module

### `:androidApp`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `MainActivity` | class | `main` | `androidApp/src/main/kotlin/com/cbgm/sparrow/MainActivity.kt` |
| `SparrowApplication` | class | `main` | `androidApp/src/main/kotlin/com/cbgm/sparrow/SparrowApplication.kt` |

### `:shared`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AndroidPlatform` | class | `androidMain` | `shared/src/androidMain/kotlin/com/cbgm/sparrow/device/Platform.android.kt` |
| `Platform` | interface | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/device/Platform.kt` |
| `AppViewModel` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/presentation/AppViewModel.kt` |
| `AppInitializationDependencies` | data class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/presentation/model/AppInitializationDependencies.kt` |
| `ForegroundRuntimeDependencies` | data class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/presentation/model/ForegroundRuntimeDependencies.kt` |
| `AttachmentConversationNameObserver` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/AttachmentConversationNameObserver.kt` |
| `ForegroundConnectionHandler` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/foreground/ForegroundConnectionHandler.kt` |
| `ForegroundRuntimeCoordinator` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/foreground/ForegroundRuntimeCoordinator.kt` |
| `ApplicationStartupRunner` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/ApplicationStartupRunner.kt` |
| `Completed` | object | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/StartupTask.kt` |
| `IdentityReady` | object | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/StartupTask.kt` |
| `IdentityRequired` | object | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/StartupTask.kt` |
| `StartupTask` | interface | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/StartupTask.kt` |
| `StartupTaskResult` | interface | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/StartupTask.kt` |
| `IdentityReadyStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/IdentityReadyStartupTask.kt` |
| `InitializeAppLanguageStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/InitializeAppLanguageStartupTask.kt` |
| `InitializeCryptoRuntimeStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/InitializeCryptoRuntimeStartupTask.kt` |
| `InitializeLocalIntelligenceStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/InitializeLocalIntelligenceStartupTask.kt` |
| `InitializeNotificationRuntimeStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/InitializeNotificationRuntimeStartupTask.kt` |
| `LoadControlPlaneConfigurationStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/LoadControlPlaneConfigurationStartupTask.kt` |
| `MaintainControlPlaneDirectoryStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/MaintainControlPlaneDirectoryStartupTask.kt` |
| `MaintainControlPlaneHealthStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/MaintainControlPlaneHealthStartupTask.kt` |
| `ObserveControlPlaneRegistrationTargetsStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/ObserveControlPlaneRegistrationTargetsStartupTask.kt` |
| `ResolveIdentityStatusStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/ResolveIdentityStatusStartupTask.kt` |
| `RestoreControlPlaneDirectoryStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/RestoreControlPlaneDirectoryStartupTask.kt` |
| `StartApprovedIdentityReconnectionObserverStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/StartApprovedIdentityReconnectionObserverStartupTask.kt` |
| `StartAttachmentConversationNameObserverStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/StartAttachmentConversationNameObserverStartupTask.kt` |
| `StartContactBlockObserverStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/StartContactBlockObserverStartupTask.kt` |
| `StartConversationNotificationCoordinatorStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/StartConversationNotificationCoordinatorStartupTask.kt` |
| `StartDirectIdentityResultObserverStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/StartDirectIdentityResultObserverStartupTask.kt` |
| `StartInvitationResultObserverStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/StartInvitationResultObserverStartupTask.kt` |
| `StartMembershipResultObserverStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/StartMembershipResultObserverStartupTask.kt` |
| `StartMessagingTransportResultObserverStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/StartMessagingTransportResultObserverStartupTask.kt` |
| `StartPendingAuthorizationCleanupStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/StartPendingAuthorizationCleanupStartupTask.kt` |
| `SynchronizeDeviceContactsStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/SynchronizeDeviceContactsStartupTask.kt` |
| `ValidateConversationWorkflowsStartupTask` | class | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/startup/task/ValidateConversationWorkflowsStartupTask.kt` |
| `IOSPlatform` | class | `iosMain` | `shared/src/iosMain/kotlin/com/cbgm/sparrow/device/Platform.ios.kt` |

### `:data:datastore`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `SparrowDataStore` | class | `commonMain` | `data/datastore/src/commonMain/kotlin/com/cbgm/sparrow/data/datastore/SparrowDataStore.kt` |
| `SparrowDataStoreEditor` | class | `commonMain` | `data/datastore/src/commonMain/kotlin/com/cbgm/sparrow/data/datastore/SparrowDataStore.kt` |
| `GroupAvatarFileStore` | class | `commonMain` | `data/datastore/src/commonMain/kotlin/com/cbgm/sparrow/data/datastore/group/GroupAvatarFileStore.kt` |

### `:feature:embedding`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `EmbeddingMathTest` | class | `androidHostTest` | `feature/embedding/src/androidHostTest/kotlin/com/cbgm/sparrow/feature/embedding/data/model/EmbeddingMathTest.kt` |
| `AndroidLocalEmbeddingModelDownloader` | class | `androidMain` | `feature/embedding/src/androidMain/kotlin/com/cbgm/sparrow/feature/embedding/data/platform/AndroidLocalEmbeddingModelDownloader.kt` |
| `AndroidLocalEmbeddingModelFiles` | class | `androidMain` | `feature/embedding/src/androidMain/kotlin/com/cbgm/sparrow/feature/embedding/data/platform/AndroidLocalEmbeddingModelFiles.kt` |
| `AndroidLocalEmbeddingModelManager` | class | `androidMain` | `feature/embedding/src/androidMain/kotlin/com/cbgm/sparrow/feature/embedding/data/platform/AndroidLocalEmbeddingModelManager.kt` |
| `MediaPipeLocalTextEmbedder` | class | `androidMain` | `feature/embedding/src/androidMain/kotlin/com/cbgm/sparrow/feature/embedding/data/platform/MediaPipeLocalTextEmbedder.kt` |
| `LocalEmbeddingModelDownloadWorker` | class | `androidMain` | `feature/embedding/src/androidMain/kotlin/com/cbgm/sparrow/feature/embedding/work/LocalEmbeddingModelDownloadWorker.kt` |
| `LocalEmbeddingModel` | object | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/data/model/LocalEmbeddingModel.kt` |
| `LocalEmbeddingModelManager` | interface | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/data/platform/LocalEmbeddingModelManager.kt` |
| `EmbeddingInputType` | enum class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/data/platform/LocalTextEmbedder.kt` |
| `LocalTextEmbedder` | interface | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/data/platform/LocalTextEmbedder.kt` |
| `LocalEmbeddingRepositoryImpl` | class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/data/repository/LocalEmbeddingRepositoryImpl.kt` |
| `LocalEmbeddingSettingsStorage` | class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/data/storage/LocalEmbeddingSettingsStorage.kt` |
| `LocalEmbeddingFeature` | enum class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/model/LocalEmbeddingFeature.kt` |
| `Downloading` | data class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/model/LocalEmbeddingModelState.kt` |
| `Failed` | data class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/model/LocalEmbeddingModelState.kt` |
| `LocalEmbeddingModelState` | interface | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/model/LocalEmbeddingModelState.kt` |
| `NotNeeded` | object | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/model/LocalEmbeddingModelState.kt` |
| `Preparing` | object | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/model/LocalEmbeddingModelState.kt` |
| `Ready` | object | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/model/LocalEmbeddingModelState.kt` |
| `LocalEmbeddingState` | data class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/model/LocalEmbeddingState.kt` |
| `LocalEmbeddingRepository` | interface | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/repository/LocalEmbeddingRepository.kt` |
| `InitializeLocalEmbeddingUseCase` | class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/usecase/InitializeLocalEmbeddingUseCase.kt` |
| `ObserveLocalEmbeddingStateUseCase` | class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/usecase/ObserveLocalEmbeddingStateUseCase.kt` |
| `SetLocalEmbeddingFeatureEnabledUseCase` | class | `commonMain` | `feature/embedding/src/commonMain/kotlin/com/cbgm/sparrow/feature/embedding/domain/usecase/SetLocalEmbeddingFeatureEnabledUseCase.kt` |

### `:feature:identity`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `SodiumIdentityKeyGeneratorTest` | class | `androidDeviceTest` | `feature/identity/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/identity/crypto/SodiumKeyGeneratorTest.kt` |
| `IdentityRepositoryImplTest` | class | `androidDeviceTest` | `feature/identity/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityRepositoryImplTest.kt` |
| `IdentityShareRepositoryImplTest` | class | `androidDeviceTest` | `feature/identity/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityShareRepositoryImplTest.kt` |
| `RemoteIdentityImportRepositoryImplIntegrationTest` | class | `androidDeviceTest` | `feature/identity/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteIdentityImportRepositoryImplIntegrationTest.kt` |
| `AndroidIdentityBackupCodecTest` | class | `androidDeviceTest` | `feature/identity/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/identity/device/AndroidIdentityBackupCodecTest.kt` |
| `AndroidPrivateKeyStorageTest` | class | `androidDeviceTest` | `feature/identity/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/identity/device/AndroidPrivateKeyStorageTest.kt` |
| `IdentityCryptoTest` | class | `androidDeviceTest` | `feature/identity/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/identity/device/IdentityCryptoTest.kt` |
| `FirebasePhoneVerificationRepository` | class | `androidMain` | `feature/identity/src/androidMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/FirebasePhoneVerificationRepository.kt` |
| `AndroidIdentityBackupCodec` | class | `androidMain` | `feature/identity/src/androidMain/kotlin/com/cbgm/sparrow/feature/identity/device/AndroidIdentityBackupCodec.kt` |
| `AndroidPrivateKeyStorage` | class | `androidMain` | `feature/identity/src/androidMain/kotlin/com/cbgm/sparrow/feature/identity/device/AndroidPrivateKeyStorage.kt` |
| `EncryptedBlob` | data class | `androidMain` | `feature/identity/src/androidMain/kotlin/com/cbgm/sparrow/feature/identity/device/AndroidPrivateKeyStorage.kt` |
| `DebugSmsNotifier` | class | `androidMain` | `feature/identity/src/androidMain/kotlin/com/cbgm/sparrow/feature/identity/device/DebugSmsNotifier.kt` |
| `IdentityAcknowledgementCrypto` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/crypto/IdentityAcknowledgementCrypto.kt` |
| `IdentityAcknowledgementPayloadEncoder` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/crypto/IdentityAcknowledgementPayloadEncoder.kt` |
| `IdentityKeyGenerator` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/crypto/IdentityKeyGenerator.kt` |
| `IdentityKeyPair` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/crypto/IdentityKeyPair.kt` |
| `SodiumIdentityAcknowledgementCrypto` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/crypto/SodiumIdentityAcknowledgementCrypto.kt` |
| `SodiumIdentityKeyGenerator` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/crypto/SodiumIdentityKeyGenerator.kt` |
| `IdentityLocalResetHandler` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/IdentityLocalResetHandler.kt` |
| `ApprovedIdentityReconnectionDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/ApprovedIdentityReconnectionDataSource.kt` |
| `IdentityBackupStatusDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityBackupStatusDataSource.kt` |
| `IdentityExchangeDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityExchangeDataSource.kt` |
| `IdentityExchangeStoreDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityExchangeStoreDataSource.kt` |
| `IdentityVerificationDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityVerificationDataSource.kt` |
| `LocalIdentityProfileDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/LocalIdentityProfileDataSource.kt` |
| `LocalIdentitySharingDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/LocalIdentitySharingDataSource.kt` |
| `LocalProfilePictureDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/LocalProfilePictureDataSource.kt` |
| `ManualIdentityExchangeDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/ManualIdentityExchangeDataSource.kt` |
| `PendingRemoteIdentityChangeDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/PendingRemoteIdentityChangeDataSource.kt` |
| `ProfilePictureFileDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/ProfilePictureFileDataSource.kt` |
| `PublicIdentityDataSource` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/PublicIdentityDataSource.kt` |
| `RemoteIdentityDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/RemoteIdentityDataSource.kt` |
| `RemoteProfilePictureDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/RemoteProfilePictureDataSource.kt` |
| `SparrowDataStorePublicIdentityDataSource` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/SparrowDataStorePublicIdentityDataSource.kt` |
| `IdentityAcceptanceReviewRequiredDtoException` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/IdentityAcceptanceReviewRequiredDtoException.kt` |
| `IdentityExchangeBindingDto` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/IdentityExchangeBindingDto.kt` |
| `IdentityExchangeStage` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/IdentityExchangeStage.kt` |
| `RemoteIdentityImportOriginDto` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/RemoteIdentityImportOriginDto.kt` |
| `StoredKeyExchangeStatusDto` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/StoredRemoteIdentityStateDto.kt` |
| `StoredVerificationStatusDto` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/StoredRemoteIdentityStateDto.kt` |
| `IdentityVerificationReceiptEncoder` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/protocol/IdentityVerificationReceiptEncoder.kt` |
| `ApprovedIdentityReconnectionRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/ApprovedIdentityReconnectionRepositoryImpl.kt` |
| `IdentityBackupRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityBackupRepositoryImpl.kt` |
| `IdentityExchangeRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityExchangeRepositoryImpl.kt` |
| `IdentityRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityRepositoryImpl.kt` |
| `IdentityShareRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityShareRepositoryImpl.kt` |
| `IdentityVerificationRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityVerificationRepositoryImpl.kt` |
| `LocalIdentityProfileRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/LocalIdentityProfileRepositoryImpl.kt` |
| `LocalIdentitySharingRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/LocalIdentitySharingRepositoryImpl.kt` |
| `LocalProfilePictureRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/LocalProfilePictureRepositoryImpl.kt` |
| `PendingRemoteIdentityChangeRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/PendingRemoteIdentityChangeRepositoryImpl.kt` |
| `RemoteIdentityImportRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteIdentityImportRepositoryImpl.kt` |
| `RemoteIdentityReadRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteIdentityReadRepositoryImpl.kt` |
| `RemoteProfilePictureRepositoryImpl` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteProfilePictureRepositoryImpl.kt` |
| `IdentityBackupCodec` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/IdentityBackupCodec.kt` |
| `IdentityExportRequest` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/IdentityBackupDocumentLauncher.kt` |
| `Cancelled` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PhoneNumberHintLauncher.kt` |
| `Failed` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PhoneNumberHintLauncher.kt` |
| `PhoneNumberHintResult` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PhoneNumberHintLauncher.kt` |
| `Selected` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PhoneNumberHintLauncher.kt` |
| `Unavailable` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PhoneNumberHintLauncher.kt` |
| `PrivateKeyStorage` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PrivateKeyStorage.kt` |
| `IdentityAcceptanceRequiresReviewException` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/error/IdentityAcceptanceRequiresReviewException.kt` |
| `ApprovedIdentityReconnection` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/ApprovedIdentityReconnection.kt` |
| `ContactVerificationStatus` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/ContactVerificationStatus.kt` |
| `DirectIdentitySetupMode` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/DirectIdentitySetupMode.kt` |
| `IdentityBackup` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityBackup.kt` |
| `IdentityBackupState` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityBackupState.kt` |
| `IdentityExchange` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchange.kt` |
| `IdentityExchangeDirection` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchange.kt` |
| `IdentityExchangeBinding` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchangeBinding.kt` |
| `IdentityExchangeClosure` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchangeClosure.kt` |
| `IdentityExchangeClosurePhase` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchangeClosure.kt` |
| `IdentityExchangeAcceptance` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInput.kt` |
| `IdentityExchangeOffer` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInput.kt` |
| `IdentityExchangeReady` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInput.kt` |
| `IdentityHandshakeState` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeState.kt` |
| `IdentityPeerState` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityPeerState.kt` |
| `IdentityResult` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityResult.kt` |
| `IdentityResultStatus` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityResult.kt` |
| `IdentityStatus` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityStatus.kt` |
| `KeyExchangeStatus` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/KeyExchangeStatus.kt` |
| `LocalProfilePicture` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/LocalProfilePicture.kt` |
| `PendingRemoteIdentityChange` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PendingRemoteIdentityChange.kt` |
| `CodeReceived` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PhoneVerificationStep.kt` |
| `CodeSent` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PhoneVerificationStep.kt` |
| `PhoneVerificationStep` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PhoneVerificationStep.kt` |
| `Verified` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PhoneVerificationStep.kt` |
| `PublicIdentity` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PublicIdentity.kt` |
| `RemoteIdentityOrigin` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemoteIdentityOrigin.kt` |
| `RemoteIdentityUpdate` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemoteIdentityUpdate.kt` |
| `RemotePeerIdentity` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemotePeerIdentity.kt` |
| `RemoteProfilePicture` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemoteProfilePicture.kt` |
| `SharedContactDetails` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/SharedIdentityPayload.kt` |
| `SharedIdentityPayload` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/SharedIdentityPayload.kt` |
| `ApprovedIdentityReconnectionRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/ApprovedIdentityReconnectionRepository.kt` |
| `DirectIdentitySetupModeRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/DirectIdentitySetupModeRepository.kt` |
| `IdentityBackupRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityBackupRepository.kt` |
| `IdentityExchangeRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityExchangeRepository.kt` |
| `IdentityRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityRepository.kt` |
| `IdentityShareRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityShareRepository.kt` |
| `IdentityVerificationRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityVerificationRepository.kt` |
| `LocalIdentityProfileRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/LocalIdentityProfileRepository.kt` |
| `LocalIdentitySharingRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/LocalIdentitySharingRepository.kt` |
| `LocalProfilePictureRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/LocalProfilePictureRepository.kt` |
| `PendingRemoteIdentityChangeRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/PendingRemoteIdentityChangeRepository.kt` |
| `PhoneVerificationRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/PhoneVerificationRepository.kt` |
| `RemoteIdentityImportRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/RemoteIdentityImportRepository.kt` |
| `RemoteIdentityReadRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/RemoteIdentityReadRepository.kt` |
| `RemoteProfilePictureRepository` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/RemoteProfilePictureRepository.kt` |
| `AcceptIdentityExchangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/AcceptIdentityExchangeUseCase.kt` |
| `AcceptRemoteIdentityHandshakeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/AcceptRemoteIdentityHandshakeUseCase.kt` |
| `AcknowledgeQueuedRecoveryInvitationUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/AcknowledgeQueuedRecoveryInvitationUseCase.kt` |
| `ApplyRemoteProfilePictureMetadataUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ApplyRemoteProfilePictureMetadataUseCase.kt` |
| `ApprovePendingRemoteIdentityChangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ApprovePendingRemoteIdentityChangeUseCase.kt` |
| `CancelIdentityExchangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CancelIdentityExchangeUseCase.kt` |
| `CloseIdentityExchangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CloseIdentityExchangeUseCase.kt` |
| `ConfirmPhoneVerificationCodeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ConfirmPhoneVerificationCodeUseCase.kt` |
| `CreateIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CreateIdentityUseCase.kt` |
| `CreateSharedIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CreateSharedIdentityUseCase.kt` |
| `DeclineIdentityExchangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DeclineIdentityExchangeUseCase.kt` |
| `DeclinePendingRemoteIdentityChangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DeclinePendingRemoteIdentityChangeUseCase.kt` |
| `DecodeSharedIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DecodeSharedIdentityUseCase.kt` |
| `DismissPendingRemoteIdentityChangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DismissPendingRemoteIdentityChangeUseCase.kt` |
| `EnsureRemoteSigningIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/EnsureRemoteSigningIdentityUseCase.kt` |
| `EstablishMutualIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/EstablishMutualIdentityUseCase.kt` |
| `FindRemoteIdentityPeerIdUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/FindRemoteIdentityPeerIdUseCase.kt` |
| `GetApprovedIdentityReconnectionUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetApprovedIdentityReconnectionUseCase.kt` |
| `GetIdentityExchangeBindingUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityExchangeBindingUseCase.kt` |
| `GetIdentityExchangeClosureUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityExchangeClosureUseCase.kt` |
| `GetIdentityPeerStateUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityPeerStateUseCase.kt` |
| `GetIdentityStatusUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityStatusUseCase.kt` |
| `GetLocalIdentityNameUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetLocalIdentityNameUseCase.kt` |
| `GetLocalPhoneNumberUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetLocalPhoneNumberUseCase.kt` |
| `GetPublicIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetPublicIdentityUseCase.kt` |
| `GetRemoteIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetRemoteIdentityUseCase.kt` |
| `HandleIdentityVerificationReceiptUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/HandleIdentityVerificationReceiptUseCase.kt` |
| `GetIdentityBackupStatusUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `MarkIdentityBackupExportedUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `PrepareIdentityBackupUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `ImportRemoteIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ImportRemoteIdentityUseCase.kt` |
| `InvalidateIdentityExchangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/InvalidateIdentityExchangeUseCase.kt` |
| `NormalizeLocalPhoneNumberUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/NormalizeLocalPhoneNumberUseCase.kt` |
| `ObserveApprovedIdentityReconnectionsUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveApprovedIdentityReconnectionsUseCase.kt` |
| `ObserveIdentityHandshakeStateUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveIdentityHandshakeStateUseCase.kt` |
| `ObserveIdentityResultsUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveIdentityResultsUseCase.kt` |
| `ObserveLocalIdentityReadyUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveLocalIdentityReadyUseCase.kt` |
| `ObserveLocalIdentitySharedUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveLocalIdentitySharedUseCase.kt` |
| `ObserveLocalProfilePictureUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveLocalProfilePictureUseCase.kt` |
| `ObservePendingRemoteIdentityChangesUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObservePendingRemoteIdentityChangesUseCase.kt` |
| `ObserveRemoteIdentitiesUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveRemoteIdentitiesUseCase.kt` |
| `ReassignIdentityExchangePeerUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReassignIdentityExchangePeerUseCase.kt` |
| `ReceiveIdentityAcknowledgementUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityAcknowledgementUseCase.kt` |
| `ReceiveIdentityExchangeAcceptedUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityExchangeAcceptedUseCase.kt` |
| `ReceiveIdentityExchangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityExchangeUseCase.kt` |
| `ReceiveIdentityReadyUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityReadyUseCase.kt` |
| `ReceiveManualIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveManualIdentityUseCase.kt` |
| `RecordLocalIdentitySharedUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecordLocalIdentitySharedUseCase.kt` |
| `RecordRemoteIdentityDeclineUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecordRemoteIdentityDeclineUseCase.kt` |
| `RecoverIncompleteIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecoverIncompleteIdentityUseCase.kt` |
| `RecoverManualIdentityExchangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecoverManualIdentityExchangeUseCase.kt` |
| `RemoveLocalProfilePictureUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RemoveLocalProfilePictureUseCase.kt` |
| `SaveLocalPhoneNameUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SaveLocalPhoneNameUseCase.kt` |
| `SendIdentityVerificationReceiptUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SendIdentityVerificationReceiptUseCase.kt` |
| `SendPhoneVerificationCodeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SendPhoneVerificationCodeUseCase.kt` |
| `SetLocalProfilePictureUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SetLocalProfilePictureUseCase.kt` |
| `StagePendingRemoteIdentityChangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StagePendingRemoteIdentityChangeUseCase.kt` |
| `StageRemoteIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StageRemoteIdentityUseCase.kt` |
| `StartIdentityExchangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StartIdentityExchangeUseCase.kt` |
| `StartManualIdentityExchangeUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StartManualIdentityExchangeUseCase.kt` |
| `VerifyRemoteIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/VerifyRemoteIdentityUseCase.kt` |
| `ReadIdentityBackupUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/restore/ReadIdentityBackupUseCase.kt` |
| `ReadIdentityRestoreDocumentUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/restore/ReadIdentityRestoreDocumentUseCase.kt` |
| `SaveRestoredIdentityUseCase` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/restore/SaveRestoredIdentityUseCase.kt` |
| `IdentityBackupViewModel` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/backup/IdentityBackupViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/backup/model/IdentityBackupUiEvent.kt` |
| `BackupFileWritten` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/backup/model/IdentityBackupUiEvent.kt` |
| `ExportIdentityClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/backup/model/IdentityBackupUiEvent.kt` |
| `IdentityBackupUiEvent` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/backup/model/IdentityBackupUiEvent.kt` |
| `PrepareBackup` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/backup/model/IdentityBackupUiEvent.kt` |
| `BackupStateUi` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/backup/model/IdentityBackupUiState.kt` |
| `IdentityBackupUiState` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/backup/model/IdentityBackupUiState.kt` |
| `IdentityCreateViewModel` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/IdentityCreateViewModel.kt` |
| `CreateClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiEvent.kt` |
| `IdentityCreateUiEvent` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiEvent.kt` |
| `NameChanged` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiEvent.kt` |
| `PhoneNumberChanged` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiEvent.kt` |
| `Completed` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiState.kt` |
| `Creating` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiState.kt` |
| `Error` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiState.kt` |
| `Form` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiState.kt` |
| `IdentityCreateUiState` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiState.kt` |
| `IncompleteIdentity` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiState.kt` |
| `Loading` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/create/model/IdentityCreateUiState.kt` |
| `IdentityDetailsViewModel` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/details/IdentityDetailsViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/details/model/IdentityDetailsUiEvent.kt` |
| `IdentityDetailsUiEvent` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/details/model/IdentityDetailsUiEvent.kt` |
| `RetryClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/details/model/IdentityDetailsUiEvent.kt` |
| `ShareIdentityClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/details/model/IdentityDetailsUiEvent.kt` |
| `IdentityDetailsUiState` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/details/model/IdentityDetailsUiState.kt` |
| `PublicHexUi` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/details/model/IdentityDetailsUiState.kt` |
| `IdentityOverviewViewModel` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/IdentityOverviewViewModel.kt` |
| `IdentityOverviewUiEvent` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/model/IdentityOverviewUiEvent.kt` |
| `OpenBackupClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/model/IdentityOverviewUiEvent.kt` |
| `OpenKeysClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/model/IdentityOverviewUiEvent.kt` |
| `RetryClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/model/IdentityOverviewUiEvent.kt` |
| `ShareIdentityClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/model/IdentityOverviewUiEvent.kt` |
| `Error` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/model/IdentityOverviewUiState.kt` |
| `IdentityOverviewUiState` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/model/IdentityOverviewUiState.kt` |
| `Loading` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/model/IdentityOverviewUiState.kt` |
| `Ready` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/model/IdentityOverviewUiState.kt` |
| `IdentityProfilePictureUiState` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/overview/profile/IdentityProfilePictureUiState.kt` |
| `IdentityRestoreViewModel` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/restore/IdentityRestoreViewModel.kt` |
| `IdentityRestoreStageUi` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/restore/model/IdentityRestoreStageUi.kt` |
| `ConfirmClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/restore/model/IdentityRestoreUiEvent.kt` |
| `DismissClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/restore/model/IdentityRestoreUiEvent.kt` |
| `IdentityRestoreUiEvent` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/restore/model/IdentityRestoreUiEvent.kt` |
| `PasswordChanged` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/restore/model/IdentityRestoreUiEvent.kt` |
| `IdentityRestoreUiState` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/restore/model/IdentityRestoreUiState.kt` |
| `ShareIdentityViewModel` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/ShareIdentityViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/model/ShareIdentityUiEvent.kt` |
| `GenerateClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/model/ShareIdentityUiEvent.kt` |
| `ShareClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/model/ShareIdentityUiEvent.kt` |
| `ShareIdentityUiEvent` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/model/ShareIdentityUiEvent.kt` |
| `ShareIdentityUiState` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/model/ShareIdentityUiState.kt` |
| `PhoneVerificationViewModel` | class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/PhoneVerificationViewModel.kt` |
| `Cancelled` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationResultUi.kt` |
| `PhoneVerificationResultUi` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationResultUi.kt` |
| `Verified` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationResultUi.kt` |
| `CodeChanged` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationUiEvent.kt` |
| `ConfirmClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationUiEvent.kt` |
| `DismissClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationUiEvent.kt` |
| `PhoneVerificationUiEvent` | interface | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationUiEvent.kt` |
| `ResendClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationUiEvent.kt` |
| `SkipClicked` | object | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationUiEvent.kt` |
| `PhoneVerificationStageUi` | enum class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationUiState.kt` |
| `PhoneVerificationUiState` | data class | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/verification/model/PhoneVerificationUiState.kt` |
| `ApprovedIdentityReconnectionRepositoryImplTest` | class | `commonTest` | `feature/identity/src/commonTest/kotlin/com/cbgm/sparrow/feature/identity/data/repository/ApprovedIdentityReconnectionRepositoryImplTest.kt` |
| `FakeDao` | class | `commonTest` | `feature/identity/src/commonTest/kotlin/com/cbgm/sparrow/feature/identity/data/repository/ApprovedIdentityReconnectionRepositoryImplTest.kt` |
| `FakeDao` | class | `commonTest` | `feature/identity/src/commonTest/kotlin/com/cbgm/sparrow/feature/identity/data/repository/PendingRemoteIdentityChangeRepositoryImplTest.kt` |
| `PendingRemoteIdentityChangeRepositoryImplTest` | class | `commonTest` | `feature/identity/src/commonTest/kotlin/com/cbgm/sparrow/feature/identity/data/repository/PendingRemoteIdentityChangeRepositoryImplTest.kt` |
| `IdentityHandshakeInputTest` | class | `commonTest` | `feature/identity/src/commonTest/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInputTest.kt` |
| `RemotePeerIdentityEncryptionTest` | class | `commonTest` | `feature/identity/src/commonTest/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemotePeerIdentityEncryptionTest.kt` |
| `IdentityRecoveryAuthorizationHoldTest` | class | `commonTest` | `feature/identity/src/commonTest/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityRecoveryAuthorizationHoldTest.kt` |

### `:feature:applock`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AppLockSettingsDataSource` | class | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/data/datasource/AppLockSettingsDataSource.kt` |
| `AppLockRepositoryImpl` | class | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/data/repository/AppLockRepositoryImpl.kt` |
| `AppLockAuthenticationResult` | interface | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/device/AppLockAuthenticationLauncher.kt` |
| `Authenticated` | object | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/device/AppLockAuthenticationLauncher.kt` |
| `Cancelled` | object | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/device/AppLockAuthenticationLauncher.kt` |
| `Failed` | data class | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/device/AppLockAuthenticationLauncher.kt` |
| `Unavailable` | object | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/device/AppLockAuthenticationLauncher.kt` |
| `AppLockRepository` | interface | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/domain/repository/AppLockRepository.kt` |
| `ObserveAppLockEnabledUseCase` | class | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/domain/usecase/ObserveAppLockEnabledUseCase.kt` |
| `SetAppLockEnabledUseCase` | class | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/domain/usecase/SetAppLockEnabledUseCase.kt` |
| `AppLockPulse` | data class | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/presentation/AppLockScreen.kt` |
| `AppLockViewModel` | class | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/presentation/AppLockViewModel.kt` |
| `AppLockUiState` | interface | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/presentation/model/AppLockUiState.kt` |
| `Loading` | object | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/presentation/model/AppLockUiState.kt` |
| `Locked` | data class | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/presentation/model/AppLockUiState.kt` |
| `Unlocked` | object | `commonMain` | `feature/applock/src/commonMain/kotlin/com/cbgm/sparrow/feature/applock/presentation/model/AppLockUiState.kt` |

### `:feature:invite`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `InvitationLifecycleDataSource` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/data/datasource/InvitationLifecycleDataSource.kt` |
| `InvitationOutboxDeliveryHandler` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/data/outbox/InvitationOutboxDeliveryHandler.kt` |
| `InvitationRepositoryImpl` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/data/repository/InvitationRepositoryImpl.kt` |
| `Invitation` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationDirection` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationPayloadType` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationStatus` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationsContext` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/Invitation.kt` |
| `InvitationLifecycleRecord` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationLifecycleRecord.kt` |
| `InvitationLifecycleStatus` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationLifecycleStatus.kt` |
| `InvitationResponse` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationResponse.kt` |
| `InvitationResult` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationResult.kt` |
| `InvitationResultAction` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/model/InvitationResultAction.kt` |
| `InvitationRepository` | interface | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/repository/InvitationRepository.kt` |
| `AcceptInvitationUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/AcceptInvitationUseCase.kt` |
| `DeclineAndBlockInvitationUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/DeclineAndBlockInvitationUseCase.kt` |
| `DeclineInvitationUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/DeclineInvitationUseCase.kt` |
| `DeleteDeclinedOutgoingInvitationUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/DeleteDeclinedOutgoingInvitationUseCase.kt` |
| `HandleInvitationResponseUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/HandleInvitationResponseUseCase.kt` |
| `InvalidatePendingInvitationUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/InvalidatePendingInvitationUseCase.kt` |
| `MarkInvitationTransportFailedUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/MarkInvitationTransportFailedUseCase.kt` |
| `MarkInvitationsViewedUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/MarkInvitationsViewedUseCase.kt` |
| `ObserveInvitationLifecycleStatusUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationLifecycleStatusUseCase.kt` |
| `ObserveInvitationResultsUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationResultsUseCase.kt` |
| `ObserveInvitationsContextUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationsContextUseCase.kt` |
| `ObserveInvitationsUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObserveInvitationsUseCase.kt` |
| `ObservePendingInvitationCountUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObservePendingInvitationCountUseCase.kt` |
| `ObservePendingInvitationsUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ObservePendingInvitationsUseCase.kt` |
| `RecordPendingInvitationUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/RecordPendingInvitationUseCase.kt` |
| `ShouldRecordPendingInvitationUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ShouldRecordPendingInvitationUseCase.kt` |
| `ValidatePendingInvitationUseCase` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/domain/usecase/ValidatePendingInvitationUseCase.kt` |
| `InvitationViewModel` | class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/InvitationViewModel.kt` |
| `InvitationStatusPresentation` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/InvitationsScreen.kt` |
| `InvitationEffect` | interface | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationEffect.kt` |
| `ShowError` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationEffect.kt` |
| `AcceptClicked` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiEvent.kt` |
| `CloseClicked` | object | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiEvent.kt` |
| `DeclineAndBlockClicked` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiEvent.kt` |
| `DeclineClicked` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiEvent.kt` |
| `DeleteDeclinedOutgoingClicked` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiEvent.kt` |
| `InvitationUiEvent` | interface | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiEvent.kt` |
| `TabSelected` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiEvent.kt` |
| `InvitationTab` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUi` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiDirection` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiPayloadType` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiState` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationUiStatus` | enum class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `InvitationsUiData` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |
| `MailboxReviewRequestUi` | data class | `commonMain` | `feature/invite/src/commonMain/kotlin/com/cbgm/sparrow/feature/invite/presentation/model/InvitationUiState.kt` |

### `:feature:contacts`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ContactRepositoryImplIntegrationTest` | class | `androidDeviceTest` | `feature/contacts/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/ContactRepositoryImplIntegrationTest.kt` |
| `AndroidDeviceContactWriterRepository` | class | `androidMain` | `feature/contacts/src/androidMain/kotlin/com/cbgm/sparrow/feature/contacts/device/AndroidDeviceContactWriterRepository.kt` |
| `AndroidDeviceContactsRepository` | class | `androidMain` | `feature/contacts/src/androidMain/kotlin/com/cbgm/sparrow/feature/contacts/device/AndroidDeviceContactsRepository.kt` |
| `SafetyNumber` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/crypto/SafetyNumber.kt` |
| `OrderedIdentities` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/crypto/SafetyNumberGenerator.kt` |
| `SafetyNumberGenerator` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/crypto/SafetyNumberGenerator.kt` |
| `ContactByRoutingIdDataSource` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactByRoutingIdDataSource.kt` |
| `ContactLocalDataSource` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactLocalDataSource.kt` |
| `ContactRoutingDataSource` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactRoutingDataSource.kt` |
| `ContactRoutingIdDataSource` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactRoutingIdDataSource.kt` |
| `ContactRoutingReconciliationDataSource` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactRoutingReconciliationDataSource.kt` |
| `MailboxContactDataSource` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/MailboxContactDataSource.kt` |
| `MailboxContactStateDto` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/MailboxContactDataSource.kt` |
| `ContactRepositoryImpl` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/ContactRepositoryImpl.kt` |
| `ResolvedContactImportDto` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/ContactRepositoryImpl.kt` |
| `ContactTransportRepositoryImpl` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/ContactTransportRepositoryImpl.kt` |
| `IdentityPeerRepositoryImpl` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/IdentityPeerRepositoryImpl.kt` |
| `BlockedContactsContext` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/BlockedContactsContext.kt` |
| `Contact` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/Contact.kt` |
| `ContactBlocklist` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactBlocklist.kt` |
| `ContactDetailsContext` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactDetailsContext.kt` |
| `ContactPhoneNumber` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactPhoneNumber.kt` |
| `ContactPhoneNumberType` | enum class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactPhoneNumberType.kt` |
| `ContactsWithProfilePictures` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactsWithProfilePictures.kt` |
| `DeviceContactLinkStatus` | enum class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/DeviceContactLinkStatus.kt` |
| `IdentityImportTrust` | enum class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/IdentityImportTrust.kt` |
| `ImportContactRequest` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ImportContactRequest.kt` |
| `ImportDeviceContactRequest` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ImportDeviceContactRequest.kt` |
| `ImportDevicePhoneNumber` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ImportDevicePhoneNumber.kt` |
| `IncomingPeerContactCandidate` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/IncomingPeerContactCandidate.kt` |
| `MailboxContactState` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/MailboxContactState.kt` |
| `SparrowIdentity` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/SparrowIdentity.kt` |
| `AddDeviceContactRequest` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `AddDeviceContactResult` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `Added` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `AlreadyExists` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `Failure` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `InvalidPhoneNumber` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `PermissionDenied` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `DeviceContact` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/DeviceContact.kt` |
| `DevicePhoneNumber` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/DevicePhoneNumber.kt` |
| `DevicePhoneNumberType` | enum class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/DevicePhoneNumberType.kt` |
| `IdentityPeerMerge` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/identity/IdentityPeerResolution.kt` |
| `IdentityPeerResolution` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/identity/IdentityPeerResolution.kt` |
| `ContactBlocklistRepository` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/ContactBlocklistRepository.kt` |
| `ContactRepository` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/ContactRepository.kt` |
| `ContactTransportRepository` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/ContactTransportRepository.kt` |
| `DeviceContactWriterRepository` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/DeviceContactWriterRepository.kt` |
| `DeviceContactsPermissionRepository` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/DeviceContactsPermissionRepository.kt` |
| `DeviceContactsRepository` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/DeviceContactsRepository.kt` |
| `IdentityPeerRepository` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/IdentityPeerRepository.kt` |
| `AddDeviceContactUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/AddDeviceContactUseCase.kt` |
| `BlockContactUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/BlockContactUseCase.kt` |
| `GetContactSafetyNumberUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetContactSafetyNumberUseCase.kt` |
| `GetContactUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetContactUseCase.kt` |
| `GetMailboxContactStatesUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetMailboxContactStatesUseCase.kt` |
| `GetMutualContactSigningPublicKeyUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetMutualContactSigningPublicKeyUseCase.kt` |
| `ImportContactUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ImportContactUseCase.kt` |
| `ImportDeviceContactsUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ImportDeviceContactsUseCase.kt` |
| `ObserveBlockedContactsContextUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveBlockedContactsContextUseCase.kt` |
| `ObserveContactBlocklistUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactBlocklistUseCase.kt` |
| `ObserveContactDetailsContextUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactDetailsContextUseCase.kt` |
| `ObserveContactUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactUseCase.kt` |
| `ObserveContactsUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactsUseCase.kt` |
| `ObserveIdentitySetupModeUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveIdentitySetupModeUseCase.kt` |
| `ReconcileContactTransportRoutingUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ReconcileContactTransportRoutingUseCase.kt` |
| `ResolveContactBootstrapRoutingIdUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactBootstrapRoutingIdUseCase.kt` |
| `ResolveContactIdByRoutingIdUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactIdByRoutingIdUseCase.kt` |
| `ResolveContactInvitationRoutingIdUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactInvitationRoutingIdUseCase.kt` |
| `ResolveContactTransportRoutingIdUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactTransportRoutingIdUseCase.kt` |
| `ResolveIncomingPeerContactsUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveIncomingPeerContactsUseCase.kt` |
| `UnblockContactUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/UnblockContactUseCase.kt` |
| `ApplyIdentityPeerMergeUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/ApplyIdentityPeerMergeUseCase.kt` |
| `GetIdentityPeerDisplayNameUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/GetIdentityPeerDisplayNameUseCase.kt` |
| `InspectContactPeerUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/InspectContactPeerUseCase.kt` |
| `UpdateIncomingIdentityPeerMetadataUseCase` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/UpdateIncomingIdentityPeerMetadataUseCase.kt` |
| `BlockedContactsActionState` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/BlockedContactsViewModel.kt` |
| `BlockedContactsFormState` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/BlockedContactsViewModel.kt` |
| `BlockedContactsViewModel` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/BlockedContactsViewModel.kt` |
| `BlockedContactsEffect` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsEffect.kt` |
| `ShowError` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsEffect.kt` |
| `AddContactClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `AddContactsDismissed` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `BlockContactClicked` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `BlockPhoneNumberClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `BlockedContactsUiEvent` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `PhoneNumberChanged` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `UnblockContactClicked` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `BlockedContactsUiState` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiState.kt` |
| `ContactDetailsContent` | enum class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/ContactDetailsRoute.kt` |
| `ContactDetailsViewModel` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/ContactDetailsViewModel.kt` |
| `VerificationActionState` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/ContactDetailsViewModel.kt` |
| `ContactDetailPage` | enum class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/components/ContactDetailsContent.kt` |
| `ContactDetailsPreviewData` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/components/ContactDetailsPreviewData.kt` |
| `ContactDetailsContactUi` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `ContactPhoneNumberTypeUi` | enum class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `ContactPhoneNumberUi` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `DeviceContactLinkUi` | enum class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `SparrowIdentityUi` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `BackClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `ConfirmVerificationClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `ContactDetailsUiEvent` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `MediaAndFilesClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `RetryClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `ScanQrCodeClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `ShareContactClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `VerificationBackClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `VerifyIdentityClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `ContactDetailsUiState` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiState.kt` |
| `Content` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiState.kt` |
| `Error` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiState.kt` |
| `Loading` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiState.kt` |
| `NotFound` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiState.kt` |
| `ContactsListMode` | enum class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/ContactsScreen.kt` |
| `ContactsViewModel` | class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/ContactsViewModel.kt` |
| `ContactGroupEntity` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactGroupEntity.kt` |
| `ContactUi` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactUi.kt` |
| `BackRequested` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsEffect.kt` |
| `ContactSelected` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsEffect.kt` |
| `ContactsEffect` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsEffect.kt` |
| `CreateGroupRequested` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsEffect.kt` |
| `ImportContactRequested` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsEffect.kt` |
| `ShowError` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsEffect.kt` |
| `AttachmentSelection` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsScreenMode.kt` |
| `ContactsScreenMode` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsScreenMode.kt` |
| `GroupSelection` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsScreenMode.kt` |
| `MemberSelection` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsScreenMode.kt` |
| `Overview` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsScreenMode.kt` |
| `BackClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `ContactClicked` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `ContactSelectionToggled` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `ContactsUiEvent` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `CreateGroupClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `DeviceContactsPermissionDenied` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `ImportContactClicked` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `ImportDeviceContacts` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `SearchQueryChanged` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `SelectionConfirmed` | object | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `SelectionTitleChanged` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `ContactsUiState` | interface | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiState.kt` |
| `Content` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiState.kt` |
| `Empty` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiState.kt` |
| `Error` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiState.kt` |
| `Loading` | data class | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiState.kt` |
| `SafetyNumberGeneratorTest` | class | `commonTest` | `feature/contacts/src/commonTest/kotlin/com/cbgm/sparrow/feature/contacts/crypto/SafetyNumberGeneratorTest.kt` |
| `ContactIdentityProjectionTest` | class | `commonTest` | `feature/contacts/src/commonTest/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ContactIdentityProjectionTest.kt` |
| `FakeContacts` | class | `commonTest` | `feature/contacts/src/commonTest/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ContactIdentityProjectionTest.kt` |
| `FakeIdentities` | class | `commonTest` | `feature/contacts/src/commonTest/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ContactIdentityProjectionTest.kt` |

### `:data:database`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `PendingIdentityReplacementTransactionTest` | class | `androidDeviceTest` | `data/database/src/androidDeviceTest/kotlin/com/cbgm/sparrow/data/database/PendingIdentityReplacementTransactionTest.kt` |
| `SparrowDatabase` | class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/SparrowDatabase.kt` |
| `SparrowDatabaseConstructor` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/SparrowDatabaseConstructor.kt` |
| `ApprovedIdentityReconnectionDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ApprovedIdentityReconnectionDao.kt` |
| `AutoReplyDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/AutoReplyDao.kt` |
| `ChatDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ChatDao.kt` |
| `ContactDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ContactDao.kt` |
| `ContactProfilePictureDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ContactProfilePictureDao.kt` |
| `ContactRoutingIdDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ContactRoutingIdDao.kt` |
| `GroupMembershipDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupMembershipDao.kt` |
| `GroupMetadataDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupMetadataDao.kt` |
| `GroupPinDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupPinDao.kt` |
| `GroupSecurityDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupSecurityDao.kt` |
| `GroupVerificationDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupVerificationDao.kt` |
| `IdentityExchangeDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/IdentityExchangeDao.kt` |
| `InvitationDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/InvitationDao.kt` |
| `LinkPreviewDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/LinkPreviewDao.kt` |
| `MailboxRouteDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MailboxRouteDao.kt` |
| `MessageAttachmentDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageAttachmentDao.kt` |
| `MessageDeliveryStatusDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageDeliveryStatusDao.kt` |
| `MessageReactionDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageReactionDao.kt` |
| `MessageRecipientStateDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageRecipientStateDao.kt` |
| `MessageSafetyDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageSafetyDao.kt` |
| `MessageSearchDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageSearchDao.kt` |
| `PendingRemoteIdentityChangeDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/PendingRemoteIdentityChangeDao.kt` |
| `ProtocolOutboxDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ProtocolOutboxDao.kt` |
| `RemoteIdentityDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/RemoteIdentityDao.kt` |
| `VoiceTranscriptDao` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/VoiceTranscriptDao.kt` |
| `ApprovedIdentityReconnectionEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ApprovedIdentityReconnectionEntity.kt` |
| `AttachmentMessageContextEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/AttachmentMessageContextEntity.kt` |
| `AutoReplyEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/AutoReplyEntity.kt` |
| `AutoReplyRecipientEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/AutoReplyRecipientEntity.kt` |
| `ContactEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactEntity.kt` |
| `ContactPhoneNumberEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactPhoneNumberEntity.kt` |
| `ContactProfilePictureEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactProfilePictureEntity.kt` |
| `ContactPublicIdentityEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactPublicIdentityEntity.kt` |
| `ContactRoutingIdEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactRoutingIdEntity.kt` |
| `ConversationEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationEntity.kt` |
| `ConversationParticipantEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationParticipantEntity.kt` |
| `ConversationParticipantRole` | enum class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationParticipantRole.kt` |
| `ConversationType` | enum class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationType.kt` |
| `GroupMemberKeyEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupMemberKeyEntity.kt` |
| `GroupMembershipEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupMembershipEntity.kt` |
| `GroupMetadataEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupMetadataEntity.kt` |
| `GroupPinEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupPinEntity.kt` |
| `GroupSecurityStateEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupSecurityStateEntity.kt` |
| `GroupVerificationPairEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupVerificationPairEntity.kt` |
| `IdentityExchangeEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/IdentityExchangeEntity.kt` |
| `InvitationEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/InvitationEntity.kt` |
| `LinkPreviewEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/LinkPreviewEntity.kt` |
| `LocalMailboxCredentialEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MailboxRouteEntities.kt` |
| `RemoteMailboxRouteEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MailboxRouteEntities.kt` |
| `MessageBlobEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageBlobEntity.kt` |
| `MessageEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageEntity.kt` |
| `MessagePartEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessagePartEntity.kt` |
| `MessageReactionEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageReactionEntity.kt` |
| `MessageRecipientStateEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageRecipientStateEntity.kt` |
| `MessageSafetyAssessmentEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageSafetyAssessmentEntity.kt` |
| `MessageSearchEmbeddingEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageSearchEmbeddingEntity.kt` |
| `MessageStructuredEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageStructuredEntity.kt` |
| `MessageTextEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageTextEntity.kt` |
| `PendingRemoteIdentityChangeEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/PendingRemoteIdentityChangeEntity.kt` |
| `ProtocolOutboxEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ProtocolOutboxEntity.kt` |
| `ProtocolOutboxFailureEventEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ProtocolOutboxFailureEventEntity.kt` |
| `VoiceTranscriptEntity` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/VoiceTranscriptEntity.kt` |
| `LocalIdentityDataResetter` | interface | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/identity/LocalIdentityDataResetter.kt` |
| `RoomLocalIdentityDataResetter` | class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/identity/LocalIdentityDataResetter.kt` |
| `RoomMailboxRouteRepository` | class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/mailbox/RoomMailboxRouteRepository.kt` |
| `ApprovedIdentityOfferMigration52To53` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/ApprovedIdentityOfferMigration52To53.kt` |
| `ApprovedIdentityReconnectionMigration51To52` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/ApprovedIdentityReconnectionMigration51To52.kt` |
| `AttachmentMessageContextMigration45To46` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/AttachmentMessageContextMigration45To46.kt` |
| `AttachmentMessageContextMigration46To47` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/AttachmentMessageContextMigration46To47.kt` |
| `GroupMemberPhoneMigration47To48` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/GroupMemberPhoneMigration47To48.kt` |
| `IdentityExchangeMigration41To42` | class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/IdentityExchangeMigration41To42.kt` |
| `IdentityExchangeMigration43To44` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/IdentityExchangeMigration43To44.kt` |
| `InvitationPeerDetailsMigration44To45` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/InvitationPeerDetailsMigration44To45.kt` |
| `MessagePartPayloadMigration53To54` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/MessagePartPayloadMigration53To54.kt` |
| `PendingRemoteIdentityChangeMigration49To50` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/PendingRemoteIdentityChangeMigration49To50.kt` |
| `PendingRemoteIdentityChangeMigration50To51` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/PendingRemoteIdentityChangeMigration50To51.kt` |
| `ProtocolOutboxFailuresMigration48To49` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/ProtocolOutboxFailuresMigration48To49.kt` |
| `ContactWithPhoneNumbersDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ContactWithPhoneNumbersDto.kt` |
| `ContactWithPublicIdentityDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ContactWithPublicIdentityDto.kt` |
| `ConversationSummaryDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ConversationSummaryDto.kt` |
| `ConversationWithMessagesDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ConversationWithMessagesDto.kt` |
| `MessageCursorDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/MessageCursorDto.kt` |
| `MessageSafetySourceDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/MessageSafetySourceDto.kt` |
| `MessageSearchSourceDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/MessageSearchSourceDto.kt` |
| `StoredMessageEmbeddingDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/StoredMessageEmbeddingDto.kt` |
| `StoredMessageSearchMatchDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/StoredMessageSearchMatchDto.kt` |
| `UnreadIncomingMessageDto` | data class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/UnreadIncomingMessageDto.kt` |
| `DefaultProtocolOutbox` | class | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/outbox/DefaultProtocolOutbox.kt` |
| `DatabaseConstants` | object | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/util/DatabaseConstants.kt` |

### `:feature:contactimport`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ImportSharedIdentityIntegrationTest` | class | `androidDeviceTest` | `feature/contactimport/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/contactimport/ImportSharedIdentityIntegrationTest.kt` |
| `TestDeviceContactWriterRepository` | object | `androidDeviceTest` | `feature/contactimport/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/contactimport/ImportSharedIdentityIntegrationTest.kt` |
| `TestIdentityExchangeRepository` | object | `androidDeviceTest` | `feature/contactimport/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/contactimport/ImportSharedIdentityIntegrationTest.kt` |
| `ImportSharedIdentityUseCase` | class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/domain/usecase/ImportSharedIdentityUseCase.kt` |
| `VerifyContactByQrUseCase` | class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/domain/usecase/VerifyContactByQrUseCase.kt` |
| `ImportIdentityViewModel` | class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/ImportIdentityViewModel.kt` |
| `IdentityImportTrustUi` | enum class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/IdentityImportTrustUi.kt` |
| `BackClicked` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiEvent.kt` |
| `EncodedIdentityChanged` | data class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiEvent.kt` |
| `ImportClicked` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiEvent.kt` |
| `ImportIdentityUiEvent` | interface | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiEvent.kt` |
| `ScanQrCodeClicked` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiEvent.kt` |
| `ScannedIdentityConfirmed` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiEvent.kt` |
| `ScannedIdentityDismissed` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiEvent.kt` |
| `ImportIdentityUiState` | data class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/importing/model/ImportIdentityUiState.kt` |
| `ScanIdentityNavigationViewModel` | class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/ScanIdentityNavigationViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/model/ScanIdentityUiEvent.kt` |
| `QrCodeScanned` | data class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/model/ScanIdentityUiEvent.kt` |
| `ScanIdentityUiEvent` | interface | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/model/ScanIdentityUiEvent.kt` |
| `ScannedIdentityPreview` | data class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/scan/model/ScannedIdentityPreview.kt` |
| `VerifyContactQrViewModel` | class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/VerifyContactQrViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiEvent.kt` |
| `ErrorDismissed` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiEvent.kt` |
| `QrCodeScanned` | data class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiEvent.kt` |
| `ScannedIdentityConfirmed` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiEvent.kt` |
| `ScannedIdentityDismissed` | object | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiEvent.kt` |
| `VerifyContactQrUiEvent` | interface | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiEvent.kt` |
| `VerifyContactQrUiState` | data class | `commonMain` | `feature/contactimport/src/commonMain/kotlin/com/cbgm/sparrow/feature/contactimport/presentation/verify/model/VerifyContactQrUiState.kt` |

### `:feature:autoreply`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AutoReplyDataSource` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/data/datasource/AutoReplyDataSource.kt` |
| `AutoReplyRepositoryImpl` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/data/repository/AutoReplyRepositoryImpl.kt` |
| `AutoReply` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/model/AutoReply.kt` |
| `AutoReplyRepository` | interface | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/repository/AutoReplyRepository.kt` |
| `ActivateAutoReplyUseCase` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ActivateAutoReplyUseCase.kt` |
| `ClaimAutoReplyForContactUseCase` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ClaimAutoReplyForContactUseCase.kt` |
| `CreateAutoReplyUseCase` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/CreateAutoReplyUseCase.kt` |
| `DeactivateAutoReplyUseCase` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/DeactivateAutoReplyUseCase.kt` |
| `DeleteAutoReplyUseCase` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/DeleteAutoReplyUseCase.kt` |
| `ObserveActiveAutoReplyUseCase` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ObserveActiveAutoReplyUseCase.kt` |
| `ObserveAutoRepliesUseCase` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ObserveAutoRepliesUseCase.kt` |
| `ReleaseAutoReplyRecipientUseCase` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/ReleaseAutoReplyRecipientUseCase.kt` |
| `UpdateAutoReplyUseCase` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/domain/usecase/UpdateAutoReplyUseCase.kt` |
| `AutoReplyViewModel` | class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/AutoReplyViewModel.kt` |
| `ActivateClicked` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `AddClicked` | object | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `AutoReplyEffect` | interface | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `AutoReplyUiEvent` | interface | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `DeleteClicked` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `EditClicked` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `EditorDismissed` | object | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `EditorNameChanged` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `EditorSaveClicked` | object | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `EditorTextChanged` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `OffClicked` | object | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `ShowSnackbar` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiEvent.kt` |
| `AutoReplyEditorUiState` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiState.kt` |
| `AutoReplyUiItem` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiState.kt` |
| `AutoReplyUiState` | data class | `commonMain` | `feature/autoreply/src/commonMain/kotlin/com/cbgm/sparrow/feature/autoreply/presentation/model/AutoReplyUiState.kt` |

### `:feature:avatar`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AvatarDataSource` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/AvatarDataSource.kt` |
| `LocalAvatarEditorDataSource` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/LocalAvatarEditorDataSource.kt` |
| `CachedAvatarImage` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/LocalAvatarImageDataSource.kt` |
| `LocalAvatarImageDataSource` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/LocalAvatarImageDataSource.kt` |
| `AvatarEditorRepositoryImpl` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/repository/AvatarEditorRepositoryImpl.kt` |
| `AvatarRepositoryImpl` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/repository/AvatarRepositoryImpl.kt` |
| `ImagePickerLauncher` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/device/ImagePicker.kt` |
| `Avatar` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/Avatar.kt` |
| `AvatarEditResult` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarEditResult.kt` |
| `AvatarEditorImage` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarEditorImage.kt` |
| `AvatarImage` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarImage.kt` |
| `AvatarTarget` | interface | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarTarget.kt` |
| `Group` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarTarget.kt` |
| `LocalUser` | object | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarTarget.kt` |
| `User` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarTarget.kt` |
| `ProfilePictureCropRegion` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/ProfilePictureCropRegion.kt` |
| `ProfilePictureSourceRect` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/ProfilePictureCropRegion.kt` |
| `AvatarEditorRepository` | interface | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/repository/AvatarEditorRepository.kt` |
| `AvatarRepository` | interface | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/repository/AvatarRepository.kt` |
| `ClearAvatarEditorUseCase` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/ClearAvatarEditorUseCase.kt` |
| `ConsumeAvatarEditResultUseCase` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/ConsumeAvatarEditResultUseCase.kt` |
| `CropAvatarEditorSourceUseCase` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/CropAvatarEditorSourceUseCase.kt` |
| `ObserveAvatarUseCase` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/ObserveAvatarUseCase.kt` |
| `PrepareAvatarEditorSourceUseCase` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/PrepareAvatarEditorSourceUseCase.kt` |
| `AvatarViewModel` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/AvatarViewModel.kt` |
| `AvatarEditorStrings` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/AvatarEditor.kt` |
| `AvatarEditorViewModel` | class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/AvatarEditorViewModel.kt` |
| `ProfilePictureCropGeometry` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/crop/ProfilePictureCropGeometry.kt` |
| `AvatarEditorUiState` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/model/AvatarEditorUiState.kt` |
| `AvatarUiState` | interface | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/model/AvatarUiState.kt` |
| `Empty` | object | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/model/AvatarUiState.kt` |
| `Error` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/model/AvatarUiState.kt` |
| `Loading` | object | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/model/AvatarUiState.kt` |
| `Ready` | data class | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/model/AvatarUiState.kt` |
| `ImagePickerDelegate` | class | `iosMain` | `feature/avatar/src/iosMain/kotlin/com/cbgm/sparrow/feature/avatar/device/ImagePicker.ios.kt` |

### `:feature:linkpreview`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `LocalLinkPreviewDataSource` | class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/datasource/LocalLinkPreviewDataSource.kt` |
| `RemoteLinkPreviewDataSource` | class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/datasource/RemoteLinkPreviewDataSource.kt` |
| `LinkPreviewDto` | data class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/model/LinkPreviewDto.kt` |
| `LinkPreviewRequestDto` | data class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/model/LinkPreviewDto.kt` |
| `LinkPreviewUnavailableException` | class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/model/LinkPreviewException.kt` |
| `LinkPreviewRepositoryImpl` | class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/data/repository/LinkPreviewRepositoryImpl.kt` |
| `LinkPreview` | data class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/domain/model/LinkPreview.kt` |
| `LinkPreviewRepository` | interface | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/domain/repository/LinkPreviewRepository.kt` |
| `GetLinkPreviewUseCase` | class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/domain/usecase/GetLinkPreviewUseCase.kt` |
| `PrefetchLinkPreviewsUseCase` | class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/domain/usecase/PrefetchLinkPreviewsUseCase.kt` |
| `LinkPreviewViewModel` | class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/LinkPreviewViewModel.kt` |
| `LinkPreviewUi` | data class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/LinkPreviewUi.kt` |
| `Error` | data class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/LinkPreviewUiState.kt` |
| `LinkPreviewUiState` | interface | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/LinkPreviewUiState.kt` |
| `Loading` | object | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/LinkPreviewUiState.kt` |
| `Success` | data class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/LinkPreviewUiState.kt` |
| `LinkPreview` | data class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/TextContentPart.kt` |
| `Text` | data class | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/TextContentPart.kt` |
| `TextContentPart` | interface | `commonMain` | `feature/linkpreview/src/commonMain/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/TextContentPart.kt` |
| `TextContentPartTest` | class | `commonTest` | `feature/linkpreview/src/commonTest/kotlin/com/cbgm/sparrow/feature/linkpreview/presentation/model/TextContentPartTest.kt` |

### `:feature:chats`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `MessageHistoryDataSource` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/datasource/MessageHistoryDataSource.kt` |
| `DirectConversationDataSource` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/datasource/DirectConversationDataSource.kt` |
| `DirectConversationReadRepositoryImpl` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/repository/DirectConversationReadRepositoryImpl.kt` |
| `DirectConversationSnapshotDto` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/direct/repository/DirectConversationReadRepositoryImpl.kt` |
| `GroupAvatarDataSource` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupAvatarDataSource.kt` |
| `GroupConversationHistoryDataSource` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupConversationHistoryDataSource.kt` |
| `GroupDescriptionDataSource` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupDescriptionDataSource.kt` |
| `GroupPinDataSource` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/datasource/GroupPinDataSource.kt` |
| `GroupLocalMembershipTimelineDto` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/mapper/GroupLocalMembershipTimelineDto.kt` |
| `GroupAvatarReadRepositoryImpl` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupAvatarReadRepositoryImpl.kt` |
| `GroupConversationReadRepositoryImpl` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupConversationReadRepositoryImpl.kt` |
| `GroupStateSnapshotDto` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupConversationReadRepositoryImpl.kt` |
| `MessageSnapshotDto` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupConversationReadRepositoryImpl.kt` |
| `GroupDescriptionReadRepositoryImpl` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupDescriptionReadRepositoryImpl.kt` |
| `GroupPinReadRepositoryImpl` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupPinReadRepositoryImpl.kt` |
| `GroupVerificationReadRepositoryImpl` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/group/repository/GroupVerificationReadRepositoryImpl.kt` |
| `ConversationOverviewDataSource` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/overview/datasource/ConversationOverviewDataSource.kt` |
| `ConversationOverviewRepositoryImpl` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/overview/repository/ConversationOverviewRepositoryImpl.kt` |
| `MessageHistoryRepositoryImpl` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/data/repository/MessageHistoryRepositoryImpl.kt` |
| `LocationShareEvent` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/LocationShareState.kt` |
| `LocationShareState` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/LocationShareState.kt` |
| `LocationShareStateMachine` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/LocationShareState.kt` |
| `MessageComposerAvailability` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageComposerAvailability.kt` |
| `MessageComposerPolicy` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageComposerAvailability.kt` |
| `MessageHistoryCursor` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageHistoryCursor.kt` |
| `MessageHistoryLoadResult` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageHistoryLoadResult.kt` |
| `MessageHistoryPolicy` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageHistoryPolicy.kt` |
| `MessageReaction` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageReaction.kt` |
| `MessageSecurity` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/MessageSecurity.kt` |
| `ContactSecurityState` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/ContactSecurityState.kt` |
| `DirectChatContext` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectChatContext.kt` |
| `DirectComposerState` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectComposerState.kt` |
| `DirectConversation` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectConversation.kt` |
| `DirectMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/DirectMessage.kt` |
| `ChatMessageType` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/ChatMessageType.kt` |
| `GroupAvatar` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupAvatar.kt` |
| `GroupAvatarMetadata` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupAvatarMetadata.kt` |
| `GroupChatContext` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupChatContext.kt` |
| `GroupComposerState` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupComposerState.kt` |
| `GroupConversation` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupConversation.kt` |
| `GroupDescription` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupDescription.kt` |
| `GroupDetailsContext` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupDetailsContext.kt` |
| `GroupMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupMessage.kt` |
| `GroupPin` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupPin.kt` |
| `GroupVerificationContext` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupVerificationState.kt` |
| `GroupVerificationMembershipStatus` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupVerificationState.kt` |
| `GroupVerificationPair` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupVerificationState.kt` |
| `GroupVerificationState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/GroupVerificationState.kt` |
| `MessageDeliveryProgress` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/group/MessageDeliveryProgress.kt` |
| `ConversationOverview` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/overview/ConversationOverview.kt` |
| `ConversationOverviewPreview` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/overview/ConversationOverview.kt` |
| `ConversationOverviewType` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/overview/ConversationOverview.kt` |
| `ConversationOverviewContext` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/model/overview/ConversationOverviewContext.kt` |
| `MessageHistoryRepository` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/MessageHistoryRepository.kt` |
| `DirectConversationReadRepository` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/direct/DirectConversationReadRepository.kt` |
| `GroupAvatarReadRepository` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupAvatarReadRepository.kt` |
| `GroupConversationReadRepository` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupConversationReadRepository.kt` |
| `GroupDescriptionReadRepository` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupDescriptionReadRepository.kt` |
| `GroupPinReadRepository` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupPinReadRepository.kt` |
| `GroupVerificationReadRepository` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/group/GroupVerificationReadRepository.kt` |
| `ConversationOverviewRepository` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/repository/overview/ConversationOverviewRepository.kt` |
| `FindMessageHistoryCursorUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/FindMessageHistoryCursorUseCase.kt` |
| `LoadOlderMessagesUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/LoadOlderMessagesUseCase.kt` |
| `EncodeContactForSharingUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/contact/EncodeContactForSharingUseCase.kt` |
| `ObserveConversationQueueAvailabilityUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/ObserveConversationQueueAvailabilityUseCase.kt` |
| `ObserveDirectChatContextUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/ObserveDirectChatContextUseCase.kt` |
| `ObserveDirectConversationUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/direct/ObserveDirectConversationUseCase.kt` |
| `LoadGroupPinnedAttachmentUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/LoadGroupPinnedAttachmentUseCase.kt` |
| `ConversationSnapshot` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupChatContextUseCase.kt` |
| `GroupChatMetadata` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupChatContextUseCase.kt` |
| `ObserveGroupChatContextUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupChatContextUseCase.kt` |
| `ObserveGroupConversationUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupConversationUseCase.kt` |
| `ObserveGroupDetailsContextUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupDetailsContextUseCase.kt` |
| `ObserveGroupExpensesUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupExpensesUseCase.kt` |
| `ObserveGroupVerificationContextUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupVerificationContextUseCase.kt` |
| `ObserveGroupVerificationUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/group/ObserveGroupVerificationUseCase.kt` |
| `ObserveConversationOverviewContextUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/overview/ObserveConversationOverviewContextUseCase.kt` |
| `ObserveConversationOverviewsUseCase` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/overview/ObserveConversationOverviewsUseCase.kt` |
| `ContactsContent` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/ContactsFlow.kt` |
| `ContactsFlowViewModel` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/ContactsFlowViewModel.kt` |
| `MorphingSendButtonShape` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/component/SendButtonShape.kt` |
| `ComposerAvailabilityUi` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/ComposerAvailabilityUi.kt` |
| `ComposerPreviewUi` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/ComposerPreviewUi.kt` |
| `Type` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/ComposerPreviewUi.kt` |
| `IndicatorUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/IndicatorUiState.kt` |
| `IndicatorUiType` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/IndicatorUiState.kt` |
| `MessageComposerUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/MessageComposerUiState.kt` |
| `MessageInputActions` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/MessageInputActions.kt` |
| `MessageInputState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/composer/model/MessageInputState.kt` |
| `ComposerDraft` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/controller/ConversationComposerController.kt` |
| `ComposerRuntime` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/controller/ConversationComposerController.kt` |
| `ConversationComposerController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/controller/ConversationComposerController.kt` |
| `ConversationErrors` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/controller/ConversationErrors.kt` |
| `ConversationHistoryController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/controller/ConversationHistoryController.kt` |
| `ConversationMediaController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/controller/ConversationMediaController.kt` |
| `ConversationVoiceController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/controller/ConversationVoiceController.kt` |
| `DirectIdentityController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/controller/DirectIdentityController.kt` |
| `SharedContactController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/controller/SharedContactController.kt` |
| `HeaderAvatarKind` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/header/model/HeaderUiModel.kt` |
| `HeaderUiModel` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/header/model/HeaderUiModel.kt` |
| `SecurityBannerState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/header/model/SecurityBannerState.kt` |
| `BubbleState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageBubble.kt` |
| `PrimaryContent` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageBubble.kt` |
| `MessageBubbleShape` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageBubbleShape.kt` |
| `DissolvingMessageListState` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageDissolve.kt` |
| `BurstItemProperties` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/component/MessageReactionBurstOverlay.kt` |
| `HistoryUiModel` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/HistoryUiModel.kt` |
| `DeliveryProgressUi` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageBubbleUi.kt` |
| `MessageBubbleUi` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageBubbleUi.kt` |
| `MessageReactionUi` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageBubbleUi.kt` |
| `MessageReplyUi` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageBubbleUi.kt` |
| `MessageContextAnchor` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageContextAnchor.kt` |
| `MessageContextUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageContextUiState.kt` |
| `MessageHistoryUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageHistoryUiState.kt` |
| `MessageReactionBurst` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageReactionBurst.kt` |
| `MessageJumpState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageSearchUiState.kt` |
| `MessageSearchTargetState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/model/MessageSearchUiState.kt` |
| `ContactsPresentation` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/CreateGroupViewModel.kt` |
| `CreateGroupActionState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/CreateGroupViewModel.kt` |
| `CreateGroupFormState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/CreateGroupViewModel.kt` |
| `CreateGroupViewModel` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/CreateGroupViewModel.kt` |
| `ContactSelected` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/ContactsFlowUiEvent.kt` |
| `ContactsFlowUiEvent` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/ContactsFlowUiEvent.kt` |
| `ImportContactClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/ContactsFlowUiEvent.kt` |
| `BackRequested` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupEffect.kt` |
| `CreateGroupEffect` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupEffect.kt` |
| `GroupCreated` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupEffect.kt` |
| `BackClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupUiEvent.kt` |
| `ContactSelected` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupUiEvent.kt` |
| `CreateClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupUiEvent.kt` |
| `CreateGroupUiEvent` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupUiEvent.kt` |
| `SearchQueryChanged` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupUiEvent.kt` |
| `TitleChanged` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupUiEvent.kt` |
| `CreateGroupConversationUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/create/model/CreateGroupUiState.kt` |
| `DetailsContent` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupDetailsFlow.kt` |
| `GroupDetailsPreviewData` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupDetailsPreviewData.kt` |
| `GroupAvatarActionState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupVerificationViewModel.kt` |
| `GroupDescriptionActionState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupVerificationViewModel.kt` |
| `GroupMemberManagementState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupVerificationViewModel.kt` |
| `GroupOverviewSnapshot` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupVerificationViewModel.kt` |
| `GroupTitleActionState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupVerificationViewModel.kt` |
| `GroupVerificationSelectionState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupVerificationViewModel.kt` |
| `GroupVerificationViewModel` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/GroupVerificationViewModel.kt` |
| `Contact` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/DetailsTarget.kt` |
| `DetailsTarget` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/DetailsTarget.kt` |
| `Group` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/DetailsTarget.kt` |
| `GroupAvatarUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupAvatarUiState.kt` |
| `GroupDescriptionUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDescriptionUiState.kt` |
| `AddGroupMembersUiEvent` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `AddMembersClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `AddMembersClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `AvatarSelected` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `ContactSelected` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `GroupDetailsUiEvent` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `LeaveGroupClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `LeaveGroupConfirmed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `LeaveGroupDismissed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `MediaAndFilesClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `MemberPromotionConfirmed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `MemberPromotionDismissed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `MemberRemovalConfirmed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `MemberRemovalDismissed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `PromoteMemberAndLeaveClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `PromoteMemberClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `RemoveGroupAvatarClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `RemoveMemberClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `SaveGroupDescriptionClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `SaveGroupTitleClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `ScanMemberQrClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `SearchQueryChanged` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `VerificationBackClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `VerifyMemberClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `VerifySelectedMemberClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiEvent.kt` |
| `Content` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiState.kt` |
| `Error` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiState.kt` |
| `GroupDetailsUiState` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiState.kt` |
| `Loading` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupDetailsUiState.kt` |
| `GroupLeavePrompt` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupLeaveUiState.kt` |
| `GroupLeaveUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupLeaveUiState.kt` |
| `GroupMemberManagementUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupMemberManagementUiState.kt` |
| `GroupTitleUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupTitleUiState.kt` |
| `GroupMemberVerificationState` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupVerificationSummaryUiState.kt` |
| `GroupMemberVerificationUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupVerificationSummaryUiState.kt` |
| `GroupVerificationSummaryUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupVerificationSummaryUiState.kt` |
| `GroupVerificationUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/model/GroupVerificationUiState.kt` |
| `DirectConversationActionsController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/DirectConversationActionsController.kt` |
| `DirectConversationStateController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/DirectConversationStateController.kt` |
| `DirectConversationViewModel` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/DirectConversationViewModel.kt` |
| `DirectIndicatorController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/DirectIndicatorController.kt` |
| `AddSharedContact` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `AttachmentError` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `CancelEdit` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `CancelReply` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `DeleteMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `DirectConversationUiEvent` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `EditMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `ForwardMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `HeaderClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `ImportIdentityClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `LoadOlderMessages` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `LocationCaptureFailed` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `LocationCaptureStarted` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `MediaSelected` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `MessageContextDismissed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `MessageContextRequested` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `MessageHistoryTargetRequested` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `MessageReactionSelected` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `MessageTextChanged` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `OpenFilePicker` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `ReplyToMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `RetryMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `SafetyWarningClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `SendClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `ShareContact` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `ShareCurrentLocation` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `ShareIdentityClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `VerifyIdentityClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `VoiceSendClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiEvent.kt` |
| `DirectConversationUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/model/DirectConversationUiState.kt` |
| `ForwardingSelectionViewModel` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/ForwardingSelectionViewModel.kt` |
| `ForwardingSelectionEffect` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionEffect.kt` |
| `TargetSelected` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionEffect.kt` |
| `ForwardingSelectionUiEvent` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionUiEvent.kt` |
| `SearchQueryChanged` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionUiEvent.kt` |
| `TargetClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionUiEvent.kt` |
| `Content` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionUiState.kt` |
| `Empty` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionUiState.kt` |
| `ForwardingSelectionUiState` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionUiState.kt` |
| `Loading` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingSelectionUiState.kt` |
| `ForwardingTargetUi` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/forwarding/model/ForwardingTargetUi.kt` |
| `GroupConversationActionsController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationActionsController.kt` |
| `AttachmentSelection` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationScreen.kt` |
| `Failed` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationStateController.kt` |
| `GroupContextObservation` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationStateController.kt` |
| `GroupConversationStateController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationStateController.kt` |
| `Loaded` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationStateController.kt` |
| `Loading` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationStateController.kt` |
| `GroupConversationViewModel` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupConversationViewModel.kt` |
| `GroupExpenseController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupExpenseController.kt` |
| `GroupIndicatorController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupIndicatorController.kt` |
| `GroupPinController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupPinController.kt` |
| `GroupPollController` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/GroupPollController.kt` |
| `ActivateExpensesClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `AddExpenseClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `AddSharedContact` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `AttachmentError` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `CancelEdit` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `CancelReply` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `CloseExpensesClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `CreatePollClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `DeleteMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `EditMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `ExpenseOverviewClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `ForwardMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `GroupConversationUiEvent` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `HeaderClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `LoadOlderMessages` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `LocationCaptureFailed` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `LocationCaptureStarted` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `MediaSelected` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `MessageContextDismissed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `MessageContextRequested` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `MessageHistoryTargetRequested` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `MessageReactionSelected` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `MessageTextChanged` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `OpenFilePicker` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `PinMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `PollCloseRequested` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `PollVoteSubmitted` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `ReplyToMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `RetryMessage` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `SafetyWarningClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `SendClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `ShareContact` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `ShareCurrentLocation` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `UnpinMessage` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `VoiceSendClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiEvent.kt` |
| `GroupConversationUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupConversationUiState.kt` |
| `GroupMemberProgressUi` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupMembershipUiState.kt` |
| `GroupMembershipUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupMembershipUiState.kt` |
| `GroupMessageUi` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupMessageUi.kt` |
| `ExpenseSummaryLoadUi` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupPinUi.kt` |
| `Expenses` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupPinUi.kt` |
| `Failed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupPinUi.kt` |
| `GroupPinUi` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupPinUi.kt` |
| `Loaded` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupPinUi.kt` |
| `Loading` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupPinUi.kt` |
| `Message` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/model/GroupPinUi.kt` |
| `OverviewViewModel` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/OverviewViewModel.kt` |
| `ConversationListItem` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/ConversationListItem.kt` |
| `LastMessagePreviewUi` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/ConversationListItem.kt` |
| `AutoReplyClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/OverviewUiEvent.kt` |
| `ChatClicked` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/OverviewUiEvent.kt` |
| `DeleteConversation` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/OverviewUiEvent.kt` |
| `ErrorDismissed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/OverviewUiEvent.kt` |
| `OverviewUiEvent` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/OverviewUiEvent.kt` |
| `OverviewUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/model/OverviewUiState.kt` |
| `GroupMemberQrVerificationViewModel` | class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/GroupMemberQrVerificationViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiEvent.kt` |
| `ConfirmClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiEvent.kt` |
| `GroupMemberQrVerificationUiEvent` | interface | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiEvent.kt` |
| `PreviewDismissed` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiEvent.kt` |
| `QrCodeScanned` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiEvent.kt` |
| `RetryClicked` | object | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiEvent.kt` |
| `GroupMemberQrVerificationError` | enum class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiState.kt` |
| `GroupMemberQrVerificationUiState` | data class | `commonMain` | `feature/chats/src/commonMain/kotlin/com/cbgm/sparrow/feature/chats/presentation/verification/model/GroupMemberQrVerificationUiState.kt` |
| `GroupLocalMembershipTimelineTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/data/group/mapper/GroupLocalMembershipTimelineTest.kt` |
| `GroupProtocolPayloadEncoderTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/data/group/security/GroupProtocolPayloadEncoderTest.kt` |
| `ConversationOverviewMapperTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/data/overview/mapper/ConversationOverviewMapperTest.kt` |
| `Consumer` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/di/ViewModelScopedControllersTest.kt` |
| `Shared` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/di/ViewModelScopedControllersTest.kt` |
| `TestViewModel` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/di/ViewModelScopedControllersTest.kt` |
| `ViewModelScopedControllersTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/di/ViewModelScopedControllersTest.kt` |
| `IdentitySnapshotEqualityTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/domain/model/direct/IdentitySnapshotEqualityTest.kt` |
| `FakeConversationOverviewRepository` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/overview/ObserveConversationOverviewContextUseCaseTest.kt` |
| `ObserveConversationOverviewContextUseCaseTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/domain/usecase/overview/ObserveConversationOverviewContextUseCaseTest.kt` |
| `MessagePartUiMapperTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/presentation/common/history/mapper/MessagePartUiMapperTest.kt` |
| `GroupVerificationUiMapperTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/presentation/details/mapper/GroupVerificationUiMapperTest.kt` |
| `DirectChatAuthorizationTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/presentation/direct/mapper/DirectChatAuthorizationTest.kt` |
| `GroupPinUiMapperTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/presentation/group/mapper/GroupPinUiMapperTest.kt` |
| `GroupVerificationSummaryTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/presentation/model/GroupVerificationSummaryTest.kt` |
| `ConversationUiMapperTest` | class | `commonTest` | `feature/chats/src/commonTest/kotlin/com/cbgm/sparrow/feature/chats/presentation/overview/mapper/ConversationUiMapperTest.kt` |

### `:feature:conversationorchestration:api`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ConversationOrchestrator` | interface | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/ConversationOrchestrator.kt` |
| `ConversationCommand` | interface | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/ConversationCommand.kt` |
| `DeleteConversation` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `DeleteMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `DirectConversationCommand` | interface | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `EditMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `MarkConversationRead` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `OpenConversation` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `ReconnectConversation` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `RetryMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `SendIndicator` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `SendMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `ToggleReaction` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/DirectConversationCommand.kt` |
| `Contact` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/ForwardMessage.kt` |
| `DirectConversation` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/ForwardMessage.kt` |
| `ForwardMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/ForwardMessage.kt` |
| `ForwardTarget` | interface | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/ForwardMessage.kt` |
| `GroupConversation` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/ForwardMessage.kt` |
| `AddMembers` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `ChangeAvatar` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `ChangeDescription` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `ChangeTitle` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `CreateGroup` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `DeleteGroup` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `GroupAdministrationCommand` | interface | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `LeaveGroup` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `PromoteMember` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `RemoveAvatar` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `RemoveMember` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `SynchronizeVerification` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `TransferAdminAndLeave` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `VerifyMember` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupAdministrationCommand.kt` |
| `ActivateExpenses` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `CloseExpenses` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `ClosePoll` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `CreateExpense` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `DeleteExpense` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `DeleteMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `EditExpense` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `EditMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `GroupMessageCommand` | interface | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `MarkConversationRead` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `PinMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `RetryMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `SendIndicator` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `SendMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `SendPaymentTransfer` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `ToggleReaction` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `UnpinMessage` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `VotePoll` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/command/GroupMessageCommand.kt` |
| `ConversationFailure` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationFailure.kt` |
| `ConversationFailureException` | class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationFailure.kt` |
| `ConversationFailureReason` | enum class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationFailure.kt` |
| `ConversationIndicator` | enum class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationIndicator.kt` |
| `ConversationOpened` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationResult.kt` |
| `ConversationResult` | interface | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationResult.kt` |
| `DirectMessageQueued` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationResult.kt` |
| `DirectMessageSent` | object | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationResult.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationResult.kt` |
| `GroupCreated` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationResult.kt` |
| `GroupMessageSent` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationResult.kt` |
| `Completed` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationState.kt` |
| `ConversationState` | interface | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationState.kt` |
| `Failed` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationState.kt` |
| `Running` | data class | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/model/ConversationState.kt` |
| `ConversationIndicatorObserver` | interface | `commonMain` | `feature/conversationorchestration/api/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/api/observer/ConversationIndicatorObserver.kt` |

### `:feature:conversationorchestration:engine`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ConversationFailureMapper` | object | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/failure/ConversationFailureMapper.kt` |
| `DirectChatAuthorizationRequiredException` | class | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/failure/DirectChatAuthorizationRequiredException.kt` |
| `ConversationWorkflow` | interface | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/machine/ConversationWorkflow.kt` |
| `Complete` | data class | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/machine/Transition.kt` |
| `Next` | data class | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/machine/Transition.kt` |
| `Reject` | data class | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/machine/Transition.kt` |
| `Transition` | interface | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/machine/Transition.kt` |
| `WorkflowCommand` | interface | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/machine/WorkflowCommand.kt` |
| `WorkflowStateMachine` | interface | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/machine/WorkflowStateMachine.kt` |
| `WorkflowStep` | interface | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/machine/WorkflowStep.kt` |
| `DefaultConversationOrchestrator` | class | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/orchestrator/DefaultConversationOrchestrator.kt` |
| `WorkflowEngine` | class | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngine.kt` |
| `WorkflowLocks` | class | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowLocks.kt` |
| `WorkflowRegistry` | class | `commonMain` | `feature/conversationorchestration/engine/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowRegistry.kt` |
| `DefaultConversationOrchestratorTest` | class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/orchestrator/DefaultConversationOrchestratorTest.kt` |
| `OpenConversationWorkflow` | class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/orchestrator/DefaultConversationOrchestratorTest.kt` |
| `Step` | data class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/orchestrator/DefaultConversationOrchestratorTest.kt` |
| `Count` | data class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Count` | data class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Counted` | data class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `CountingMachine` | class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Finish` | object | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Finished` | object | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Negative` | object | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Outcome` | interface | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Step` | interface | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Step` | object | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Step` | data class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Suspend` | data class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `SuspendingMachine` | class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `Throw` | object | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `ThrowingMachine` | class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |
| `WorkflowEngineTest` | class | `commonTest` | `feature/conversationorchestration/engine/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/engine/runner/WorkflowEngineTest.kt` |

### `:feature:conversationorchestration:delivery`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `DirectDeliveryDataSource` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/data/datasource/DirectDeliveryDataSource.kt` |
| `GroupRecipientDeliveryDataSource` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/data/datasource/GroupRecipientDeliveryDataSource.kt` |
| `MessageConversationDataSource` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/data/datasource/MessageConversationDataSource.kt` |
| `WebSocketIncomingEnvelopeGateway` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/data/datasource/WebSocketIncomingEnvelopeGateway.kt` |
| `DirectMessageDeliveryRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/data/repository/DirectMessageDeliveryRepositoryImpl.kt` |
| `GroupRecipientDeliveryRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/data/repository/GroupRecipientDeliveryRepositoryImpl.kt` |
| `MessageConversationRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/data/repository/MessageConversationRepositoryImpl.kt` |
| `ApplyDeliveryEvent` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/command/DeliveryCommand.kt` |
| `DeliveryCommand` | interface | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/command/DeliveryCommand.kt` |
| `RecordPreparedTransport` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/command/DeliveryCommand.kt` |
| `DeliveryEventTarget` | interface | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/DeliveryEventTarget.kt` |
| `DirectMessage` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/DeliveryEventTarget.kt` |
| `Packet` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/DeliveryEventTarget.kt` |
| `Recipient` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/DeliveryEventTarget.kt` |
| `DirectMessageDeliveryStateMachine` | object | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/DirectMessageDeliveryStateMachine.kt` |
| `GroupMessageDeliveryStateMachine` | object | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/GroupMessageDeliveryStateMachine.kt` |
| `GroupRecipientDelivery` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/GroupRecipientDelivery.kt` |
| `MessageDeliveryEvent` | enum class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/MessageDeliveryEvent.kt` |
| `DirectMessageDeliveryRepository` | interface | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/repository/DirectMessageDeliveryRepository.kt` |
| `GroupRecipientDeliveryRepository` | interface | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/repository/GroupRecipientDeliveryRepository.kt` |
| `MessageConversationRepository` | interface | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/repository/MessageConversationRepository.kt` |
| `SendConversationIndicatorUseCase` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/usecase/SendConversationIndicatorUseCase.kt` |
| `ApplyDeliveryEventWorkflow` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `DirectMessageFound` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `FindDirectMessage` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `FindGroupRecipient` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `GroupRecipientFound` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `GroupRecipientUpdated` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `NoDeliveryOwner` | object | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `NoGroupRecipient` | object | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `StatusUpdated` | object | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `UpdateDirectMessage` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `UpdateGroupMessageStatus` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `UpdateGroupRecipient` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/event/ApplyDeliveryEventWorkflow.kt` |
| `DirectMessageFound` | object | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/transport/RecordPreparedTransportWorkflow.kt` |
| `FindDirectMessage` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/transport/RecordPreparedTransportWorkflow.kt` |
| `NotDirectMessage` | object | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/transport/RecordPreparedTransportWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/transport/RecordPreparedTransportWorkflow.kt` |
| `RecordPreparedTransportWorkflow` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/transport/RecordPreparedTransportWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/transport/RecordPreparedTransportWorkflow.kt` |
| `StorePreparedTransport` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/transport/RecordPreparedTransportWorkflow.kt` |
| `Stored` | object | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/workflow/transport/RecordPreparedTransportWorkflow.kt` |
| `OutboxDeliveryStateRouter` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/delivery/OutboxDeliveryStateRouter.kt` |
| `TransportConversationIndicatorObserver` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/indicator/TransportConversationIndicatorObserver.kt` |
| `WebSocketMessagingIndicatorGateway` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/indicator/WebSocketMessagingIndicatorGateway.kt` |
| `DefaultMailboxCapabilityLifecycle` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/DefaultMailboxCapabilityLifecycle.kt` |
| `DefaultMailboxCoordinator` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/DefaultMailboxCoordinator.kt` |
| `MailboxCredentialFactory` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/MailboxCredentialFactory.kt` |
| `MailboxPendingSynchronizer` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/MailboxPendingSynchronizer.kt` |
| `MailboxRoutePacketHandler` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/MailboxRoutePacketHandler.kt` |
| `MailboxRouteProvisioner` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/MailboxRouteProvisioner.kt` |
| `OutgoingPacketSender` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/outbox/OutgoingPacketSender.kt` |
| `OutgoingPacketTransportPolicy` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/outbox/OutgoingPacketTransportPolicy.kt` |
| `OutgoingRecipientRoutingResolver` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/outbox/OutgoingRecipientRoutingResolver.kt` |
| `OutgoingTransportPayloadFactory` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/outbox/OutgoingTransportPayloadFactory.kt` |
| `OutgoingTransportRequirement` | data class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/outbox/OutgoingTransportRequirement.kt` |
| `RetryableWireDeliveryException` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/outbox/RetryableWireDeliveryException.kt` |
| `GroupRoutingResolver` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/routing/GroupRoutingResolver.kt` |
| `GroupTransportKeyResolver` | class | `commonMain` | `feature/conversationorchestration/delivery/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/routing/GroupTransportKeyResolver.kt` |
| `DirectMessageDeliveryStateMachineTest` | class | `commonTest` | `feature/conversationorchestration/delivery/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/DirectMessageDeliveryStateMachineTest.kt` |
| `GroupMessageDeliveryStateMachineTest` | class | `commonTest` | `feature/conversationorchestration/delivery/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/domain/model/GroupMessageDeliveryStateMachineTest.kt` |
| `ConnectTimeoutException` | class | `commonTest` | `feature/conversationorchestration/delivery/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/DefaultMailboxCapabilityLifecycleTest.kt` |
| `DefaultMailboxCapabilityLifecycleTest` | class | `commonTest` | `feature/conversationorchestration/delivery/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/DefaultMailboxCapabilityLifecycleTest.kt` |
| `FakeMailboxGateway` | class | `commonTest` | `feature/conversationorchestration/delivery/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/DefaultMailboxCapabilityLifecycleTest.kt` |
| `FakeMailboxRouteRepository` | class | `commonTest` | `feature/conversationorchestration/delivery/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/DefaultMailboxCapabilityLifecycleTest.kt` |
| `MailboxRouteNodeRetirementTest` | class | `commonTest` | `feature/conversationorchestration/delivery/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/mailbox/MailboxRouteNodeRetirementTest.kt` |
| `IdentityRecoveryOutboundGateTest` | class | `commonTest` | `feature/conversationorchestration/delivery/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/outbox/IdentityRecoveryOutboundGateTest.kt` |
| `OutgoingPacketTransportPolicyTest` | class | `commonTest` | `feature/conversationorchestration/delivery/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/delivery/runtime/outbox/OutgoingPacketTransportPolicyTest.kt` |

### `:feature:conversationorchestration:message`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ConversationDataSource` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/datasource/ConversationDataSource.kt` |
| `DirectMessageDataSource` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/datasource/DirectMessageDataSource.kt` |
| `GroupMessageDataSource` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/datasource/GroupMessageDataSource.kt` |
| `MessageReactionDataSource` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/datasource/MessageReactionDataSource.kt` |
| `DirectMessagePacketFactoryImpl` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/packet/DirectMessagePacketFactoryImpl.kt` |
| `GroupMessagePlaintextCodecImpl` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/packet/GroupMessagePlaintextCodecImpl.kt` |
| `ConversationUnseenCountRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/repository/ConversationUnseenCountRepositoryImpl.kt` |
| `DirectConversationRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/repository/DirectConversationRepositoryImpl.kt` |
| `DirectMessageRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/repository/DirectMessageRepositoryImpl.kt` |
| `GroupMessageRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/repository/GroupMessageRepositoryImpl.kt` |
| `MessageReactionRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/data/repository/MessageReactionRepositoryImpl.kt` |
| `ActivateAuthorizedConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/command/MessageCommand.kt` |
| `DeliverDirectMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/command/MessageCommand.kt` |
| `DiscardWaitingDirectMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/command/MessageCommand.kt` |
| `FlushQueuedGroupMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/command/MessageCommand.kt` |
| `MessageCommand` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/command/MessageCommand.kt` |
| `ReleaseWaitingDirectMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/command/MessageCommand.kt` |
| `ConversationInfo` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/ConversationInfo.kt` |
| `DirectConversationTarget` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/DirectConversationTarget.kt` |
| `DirectInvitationState` | enum class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/DirectInvitationState.kt` |
| `DirectPendingAuthorizationMessagePolicy` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/DirectPendingAuthorizationMessagePolicy.kt` |
| `DirectRecipient` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/DirectRecipient.kt` |
| `Denied` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/GroupMessageAccess.kt` |
| `Granted` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/GroupMessageAccess.kt` |
| `GroupMessageAccess` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/GroupMessageAccess.kt` |
| `GroupMessagePlaintext` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/GroupMessagePlaintext.kt` |
| `Message` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/GroupMessagePlaintext.kt` |
| `Operation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/GroupMessagePlaintext.kt` |
| `GroupRecipientState` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/GroupRecipientState.kt` |
| `OutgoingMessageContent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/OutgoingMessageContent.kt` |
| `StoredMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/StoredMessage.kt` |
| `StoredOutgoingMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/StoredOutgoingMessage.kt` |
| `UnreadIncomingMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/UnreadIncomingMessage.kt` |
| `DirectMessagePacketFactory` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/packet/DirectMessagePacketFactory.kt` |
| `GroupMessagePlaintextCodec` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/packet/GroupMessagePlaintextCodec.kt` |
| `OutgoingMessageContentPolicy` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/policy/OutgoingMessageContentPolicy.kt` |
| `DirectReauthorizationPort` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/port/DirectReauthorizationPort.kt` |
| `ConversationUnseenCountRepository` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/repository/ConversationUnseenCountRepository.kt` |
| `DirectConversationRepository` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/repository/DirectConversationRepository.kt` |
| `DirectMessageRepository` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/repository/DirectMessageRepository.kt` |
| `GroupMessageRepository` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/repository/GroupMessageRepository.kt` |
| `MessageReactionRepository` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/repository/MessageReactionRepository.kt` |
| `CheckGroupMessageAccessUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/CheckGroupMessageAccessUseCase.kt` |
| `DeleteLocalExpenseReceiptsUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/DeleteLocalExpenseReceiptsUseCase.kt` |
| `DiscardDirectMessagesUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/DiscardDirectMessagesUseCase.kt` |
| `DiscardGroupMessagesUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/DiscardGroupMessagesUseCase.kt` |
| `EncryptGroupMessageUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/EncryptGroupMessageUseCase.kt` |
| `EncryptGroupOperationUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/EncryptGroupOperationUseCase.kt` |
| `EncryptGroupPayloadUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/EncryptGroupPayloadUseCase.kt` |
| `EnqueueGroupPacketsUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/EnqueueGroupPacketsUseCase.kt` |
| `FindDirectAuthorizationBlockUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/FindDirectAuthorizationBlockUseCase.kt` |
| `FindGroupMessageUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/FindGroupMessageUseCase.kt` |
| `FindGroupPollUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/FindGroupPollUseCase.kt` |
| `FindGroupRecipientsUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/FindGroupRecipientsUseCase.kt` |
| `InspectDirectInvitationUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/InspectDirectInvitationUseCase.kt` |
| `LoadDirectRecipientUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/LoadDirectRecipientUseCase.kt` |
| `RecordPacketSendFailureUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/RecordPacketSendFailureUseCase.kt` |
| `RecordRetryRequestedUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/RecordRetryRequestedUseCase.kt` |
| `RequireDirectChatAuthorizationUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/RequireDirectChatAuthorizationUseCase.kt` |
| `RequireMessageWithoutPaymentUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/RequireMessageWithoutPaymentUseCase.kt` |
| `SendDirectOperationUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/SendDirectOperationUseCase.kt` |
| `SendReadReceiptUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/SendReadReceiptUseCase.kt` |
| `StoreOutgoingDirectMessageUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/StoreOutgoingDirectMessageUseCase.kt` |
| `StoreOutgoingGroupMessageUseCase` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/usecase/StoreOutgoingGroupMessageUseCase.kt` |
| `Authorized` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `CheckAuthorization` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `ConversationFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `DeleteDirectMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `DeleteSent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `DiscardMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `FindOwnMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `MessageDiscarded` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `MessageNeverSent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `MessageSent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `SendDelete` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeleteDirectMessageWorkflow.kt` |
| `AuthorizationMissing` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `Authorized` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `CheckAuthorization` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `ConversationFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `CreatePacket` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `DeliverDirectMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `DiscardMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `EnqueueFailed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `EnqueuePacket` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `LinkPacket` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `LoadRecipient` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `MarkSendFailed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `MessageDiscarded` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `MessageStored` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `PacketCreated` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `PacketEnqueued` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `PacketLinked` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `PacketPreparationFailed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `RecipientLoaded` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `SendFailureRecorded` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `StoreMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/DeliverDirectMessageWorkflow.kt` |
| `Authorized` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `CheckAuthorization` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `ConversationFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `EditDirectMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `EditSent` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `FindEditableMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `MessageSent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `MessageWaitingForAuthorization` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `ReplaceText` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `SendEdit` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `TextReplaced` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/EditDirectMessageWorkflow.kt` |
| `Authorized` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `CheckAuthorization` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `ClearUnseenCount` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `ConversationFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `FindUnreadMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `MarkDirectConversationReadWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `MarkReceiptSent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `ReadReceiptSent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `ReceiptMarked` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `ReceiptsDeferred` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `SendReadReceipt` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `UnreadMessagesFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `UnseenCountCleared` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/MarkDirectConversationReadWorkflow.kt` |
| `Authorized` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `CheckAuthorization` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `CheckPacketIdentity` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `ConversationFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `FindRetryableMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `PacketCurrent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `PacketResent` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `RecordRetry` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `ResendPacket` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `RetryDirectMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `RetryRecorded` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `RetryableMessageFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/RetryDirectMessageWorkflow.kt` |
| `IndicatorSent` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectIndicatorWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectIndicatorWorkflow.kt` |
| `SendDirectIndicatorWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectIndicatorWorkflow.kt` |
| `SendIndicator` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectIndicatorWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectIndicatorWorkflow.kt` |
| `AuthorizationMissing` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `Authorized` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `CheckAuthorization` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `CheckBlockedContact` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `CheckPendingIdentityChange` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `ContactBlocked` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `ContactNotBlocked` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `ContentValidated` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `ConversationResolved` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `DeliverMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `DeliveryPlan` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `FindQueueConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `IdentityChangePending` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `InspectInvitation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `InvitationPending` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `InvitationRetryable` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `InvitationUnavailable` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `LoadQueueRecipient` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `MessageDelivered` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `MessageQueued` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `NoIdentityChangePending` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `Queue` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `QueueConversationFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `QueueRecipientLoaded` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `ReauthorizationFailed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `Reauthorized` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `RequestReauthorization` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `ResolveConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `Send` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `SendDirectMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `StoreWaitingMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `ValidateContent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/SendDirectMessageWorkflow.kt` |
| `AddReaction` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `Authorized` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `CheckAuthorization` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `ConversationFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `FindMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `ReactionAbsent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `ReactionChanged` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `ReactionPresent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `ReactionSent` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `RemoveReaction` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `SendReaction` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `ToggleDirectReactionWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/ToggleDirectReactionWorkflow.kt` |
| `ActivateAuthorizedConversationWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ActivateAuthorizedConversationWorkflow.kt` |
| `ConversationReady` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ActivateAuthorizedConversationWorkflow.kt` |
| `EnsureConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ActivateAuthorizedConversationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ActivateAuthorizedConversationWorkflow.kt` |
| `ReleaseWaitingMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ActivateAuthorizedConversationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ActivateAuthorizedConversationWorkflow.kt` |
| `WaitingMessagesReleased` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ActivateAuthorizedConversationWorkflow.kt` |
| `DiscardMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/DiscardWaitingDirectMessagesWorkflow.kt` |
| `DiscardWaitingDirectMessagesWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/DiscardWaitingDirectMessagesWorkflow.kt` |
| `FindWaitingMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/DiscardWaitingDirectMessagesWorkflow.kt` |
| `MessagesDiscarded` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/DiscardWaitingDirectMessagesWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/DiscardWaitingDirectMessagesWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/DiscardWaitingDirectMessagesWorkflow.kt` |
| `WaitingMessagesFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/DiscardWaitingDirectMessagesWorkflow.kt` |
| `Authorized` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `CheckAuthorization` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `DiscardExpiredMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `EnqueueFailed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `EnqueuePacket` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `ExpiredMessagesDiscarded` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `FindWaitingMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `MarkSendFailed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `PacketEnqueued` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `PrepareRelease` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `ReleaseDeferred` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `ReleasePrepared` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `ReleaseWaitingDirectMessagesWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `RestoreWaitingMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `SendFailureRecorded` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `WaitingMessageRestored` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `WaitingMessagesFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/direct/authorization/ReleaseWaitingDirectMessagesWorkflow.kt` |
| `ContactFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `DirectConversationFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `DirectMessageSent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `FindContact` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `FindDirectConversation` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `ForwardMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `GroupMessageSent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `IncrementUnseen` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `PartsValid` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `SendDirectMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `SendGroupMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `UnseenIncremented` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `ValidateParts` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/forward/ForwardMessageWorkflow.kt` |
| `CheckClosePermission` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `CheckMembership` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `CloseAllowed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `CloseEncrypted` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `CloseEnqueued` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `CloseGroupPollWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `CloseNotAllowed` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `ClosedPollSaved` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `EncryptClose` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `EnqueueClose` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `FindPoll` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `FindRecipients` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `MembershipActive` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `MembershipInactive` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `PollFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `RecipientsFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `SaveClosedPoll` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/CloseGroupPollWorkflow.kt` |
| `CheckMembership` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `DeleteEncrypted` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `DeleteEnqueued` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `DeleteGroupMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `DiscardMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `EncryptDelete` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `EnqueueDelete` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `FindOwnMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `FindRecipients` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `MembershipActive` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `MembershipInactive` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `MessageDiscarded` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `OwnMessageFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `RecipientsFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/DeleteGroupMessageWorkflow.kt` |
| `CheckMembership` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `EditEncrypted` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `EditEnqueued` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `EditGroupMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `EncryptEdit` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `EnqueueEdit` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `FindEditableMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `FindRecipients` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `MembershipActive` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `MembershipInactive` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `MessageEditable` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `RecipientsFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `ReplaceText` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `TextReplaced` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/EditGroupMessageWorkflow.kt` |
| `EncryptMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `EnqueuePackets` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `FindMissingRecipients` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `FindQueuedMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `FindRecipients` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `FlushQueue` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `FlushQueuedGroupMessagesWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `MessageEncrypted` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `MessageFlushed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `MessageStaysQueued` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `MissingRecipientsFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `QueuedMessagesFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `RecipientStatesSaved` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `RecipientsFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `SaveRecipientStates` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/FlushQueuedGroupMessagesWorkflow.kt` |
| `AcknowledgeMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/MarkGroupConversationReadWorkflow.kt` |
| `ClearUnseenCount` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/MarkGroupConversationReadWorkflow.kt` |
| `FindUnreadMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/MarkGroupConversationReadWorkflow.kt` |
| `MarkGroupConversationReadWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/MarkGroupConversationReadWorkflow.kt` |
| `MessageAcknowledged` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/MarkGroupConversationReadWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/MarkGroupConversationReadWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/MarkGroupConversationReadWorkflow.kt` |
| `UnreadMessagesFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/MarkGroupConversationReadWorkflow.kt` |
| `UnseenCountCleared` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/MarkGroupConversationReadWorkflow.kt` |
| `FailedRecipientsFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/RetryGroupMessageWorkflow.kt` |
| `FindFailedRecipients` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/RetryGroupMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/RetryGroupMessageWorkflow.kt` |
| `RecipientAttempted` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/RetryGroupMessageWorkflow.kt` |
| `ResendToRecipient` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/RetryGroupMessageWorkflow.kt` |
| `RetryGroupMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/RetryGroupMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/RetryGroupMessageWorkflow.kt` |
| `IndicatorSent` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupIndicatorWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupIndicatorWorkflow.kt` |
| `SendGroupIndicatorWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupIndicatorWorkflow.kt` |
| `SendIndicator` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupIndicatorWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupIndicatorWorkflow.kt` |
| `CheckMembership` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `ContentValidated` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `DiscardMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `EncryptMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `EncryptionFailed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `EnqueuePackets` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `FindRecipients` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `FlushQueuedMessages` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `MembershipActive` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `MembershipInactive` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `MessageDiscarded` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `MessageEncrypted` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `MessageStored` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `PacketsEnqueued` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `QueueFlushed` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `RecipientStatesSaved` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `RecipientsFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `SaveRecipientStates` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `SendGroupMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `StoreMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `ValidateContent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/SendGroupMessageWorkflow.kt` |
| `AddReaction` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `CheckMembership` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `EncryptReaction` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `EnqueueReaction` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `FindLocalReaction` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `FindMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `FindRecipients` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `MembershipActive` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `MembershipInactive` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `MessageFound` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `ReactionAbsent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `ReactionChanged` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `ReactionEncrypted` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `ReactionEnqueued` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `ReactionPresent` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `RecipientsFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `RemoveReaction` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `ToggleGroupReactionWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/ToggleGroupReactionWorkflow.kt` |
| `CheckMembership` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `EncryptVote` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `EnqueueVote` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `FindPoll` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `FindPollMessage` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `FindRecipients` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `MembershipActive` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `MembershipInactive` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `PollFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `PollMessageFound` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `RecipientsFound` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `SaveVote` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `VoteEncrypted` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `VoteEnqueued` | object | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `VoteGroupPollWorkflow` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `VoteSaved` | data class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/workflow/group/VoteGroupPollWorkflow.kt` |
| `PendingAuthorizationCleanupRunner` | class | `commonMain` | `feature/conversationorchestration/message/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/runtime/PendingAuthorizationCleanupRunner.kt` |
| `DirectPendingAuthorizationMessagePolicyTest` | class | `commonTest` | `feature/conversationorchestration/message/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/message/domain/model/DirectPendingAuthorizationMessagePolicyTest.kt` |

### `:feature:conversationorchestration:groupstate`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `GroupAvatarDataSource` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/datasource/GroupAvatarDataSource.kt` |
| `GroupDescriptionDataSource` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/datasource/GroupDescriptionDataSource.kt` |
| `GroupPinDataSource` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/datasource/GroupPinDataSource.kt` |
| `GroupTitleDataSource` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/datasource/GroupTitleDataSource.kt` |
| `GroupAvatarDto` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/model/GroupMetadataDtos.kt` |
| `GroupDescriptionDto` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/model/GroupMetadataDtos.kt` |
| `GroupTitleDto` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/model/GroupMetadataDtos.kt` |
| `GroupAvatarPacketCodecImpl` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/packet/GroupAvatarPacketCodecImpl.kt` |
| `GroupDescriptionPacketCodecImpl` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/packet/GroupDescriptionPacketCodecImpl.kt` |
| `GroupPinPacketCodecImpl` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/packet/GroupPinPacketCodecImpl.kt` |
| `GroupTitlePacketCodecImpl` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/packet/GroupTitlePacketCodecImpl.kt` |
| `GroupAvatarRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/repository/GroupAvatarRepositoryImpl.kt` |
| `GroupDescriptionRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/repository/GroupDescriptionRepositoryImpl.kt` |
| `GroupPinRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/repository/GroupPinRepositoryImpl.kt` |
| `GroupTitleRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/data/repository/GroupTitleRepositoryImpl.kt` |
| `GroupStateCommand` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/command/GroupStateCommand.kt` |
| `PublishGroupAvatar` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/command/GroupStateCommand.kt` |
| `SendCurrentGroupExpenses` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/command/GroupStateCommand.kt` |
| `SendCurrentGroupMetadata` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/command/GroupStateCommand.kt` |
| `SendCurrentGroupPin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/command/GroupStateCommand.kt` |
| `GroupAdminContext` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/model/GroupAdminContext.kt` |
| `GroupAvatarState` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/model/GroupAvatarState.kt` |
| `GroupDescriptionState` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/model/GroupDescriptionState.kt` |
| `GroupExpenseMembers` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/model/GroupExpenseMembers.kt` |
| `GroupPinState` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/model/GroupPinState.kt` |
| `GroupTitleState` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/model/GroupTitleState.kt` |
| `GroupAvatarPacketCodec` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/packet/GroupAvatarPacketCodec.kt` |
| `GroupDescriptionPacketCodec` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/packet/GroupDescriptionPacketCodec.kt` |
| `GroupPinPacketCodec` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/packet/GroupPinPacketCodec.kt` |
| `GroupTitlePacketCodec` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/packet/GroupTitlePacketCodec.kt` |
| `GroupAvatarRepository` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/repository/GroupAvatarRepository.kt` |
| `GroupDescriptionRepository` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/repository/GroupDescriptionRepository.kt` |
| `GroupPinRepository` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/repository/GroupPinRepository.kt` |
| `GroupTitleRepository` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/repository/GroupTitleRepository.kt` |
| `ClearGroupStateUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/ClearGroupStateUseCase.kt` |
| `CreateGroupAvatarPacketsUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/CreateGroupAvatarPacketsUseCase.kt` |
| `CreateGroupDescriptionPacketsUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/CreateGroupDescriptionPacketsUseCase.kt` |
| `CreateGroupPinPacketsUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/CreateGroupPinPacketsUseCase.kt` |
| `CreateGroupTitlePacketsUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/CreateGroupTitlePacketsUseCase.kt` |
| `FindGroupExpenseMembersUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/FindGroupExpenseMembersUseCase.kt` |
| `RequireGroupAdminContextUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/RequireGroupAdminContextUseCase.kt` |
| `RequireGroupPaymentTransferUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/RequireGroupPaymentTransferUseCase.kt` |
| `SendCurrentGroupAvatarUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/SendCurrentGroupAvatarUseCase.kt` |
| `SendCurrentGroupDescriptionUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/SendCurrentGroupDescriptionUseCase.kt` |
| `SendCurrentGroupTitleUseCase` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/usecase/SendCurrentGroupTitleUseCase.kt` |
| `ActivateGroupExpensesWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `AdminAuthorized` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `BoardCreated` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `BoardPinned` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `BoardSent` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `CheckNoActiveBoard` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `CreateBoard` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `NoActiveBoard` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `PinBoard` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `SendBoard` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/ActivateGroupExpensesWorkflow.kt` |
| `CreateExpense` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/CreateGroupExpenseWorkflow.kt` |
| `CreateGroupExpenseWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/CreateGroupExpenseWorkflow.kt` |
| `ExpenseCreated` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/CreateGroupExpenseWorkflow.kt` |
| `ExpenseMembersFound` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/CreateGroupExpenseWorkflow.kt` |
| `ExpenseSent` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/CreateGroupExpenseWorkflow.kt` |
| `FindExpenseMembers` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/CreateGroupExpenseWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/CreateGroupExpenseWorkflow.kt` |
| `SendExpense` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/CreateGroupExpenseWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/CreateGroupExpenseWorkflow.kt` |
| `DeleteGroupExpenseWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `DeleteReceipts` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `ExpenseMarkedDeleted` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `ExpenseMembersFound` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `ExpenseSent` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `FindExpenseMembers` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `MarkExpenseDeleted` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `ReceiptsDeleted` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `SendExpense` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/DeleteGroupExpenseWorkflow.kt` |
| `DeleteReplacedReceipts` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `EditGroupExpenseWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `ExpenseMembersFound` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `ExpenseRevised` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `ExpenseSent` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `FindExpenseMembers` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `ReplacedReceiptsDeleted` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `ReviseExpense` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `SendExpense` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/EditGroupExpenseWorkflow.kt` |
| `BoardFound` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `EnqueueExpenseMessages` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `ExpenseMessage` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `ExpenseMessagesEnqueued` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `ExpenseMessagesLoaded` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `FindBoard` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `LoadExpenseMessages` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `NoOpenBoard` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `SendCurrentGroupExpensesWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/expense/SendCurrentGroupExpensesWorkflow.kt` |
| `AvatarPublished` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupAvatarWorkflow.kt` |
| `AvatarValid` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupAvatarWorkflow.kt` |
| `ChangeGroupAvatarWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupAvatarWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupAvatarWorkflow.kt` |
| `PublishAvatar` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupAvatarWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupAvatarWorkflow.kt` |
| `ValidateAvatar` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupAvatarWorkflow.kt` |
| `AdminAuthorized` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `ChangeGroupDescriptionWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `CreateDescriptionPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `DescriptionLoaded` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `DescriptionPacketsCreated` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `DescriptionPacketsEnqueued` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `DescriptionSaved` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `DescriptionUnchanged` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `EnqueueDescriptionPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `LoadDescription` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `SaveDescription` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupDescriptionWorkflow.kt` |
| `AdminAuthorized` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `ChangeGroupTitleWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `CreateTitlePackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `EnqueueTitlePackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `LoadTitle` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `SaveTitle` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `TitleLoaded` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `TitlePacketsCreated` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `TitlePacketsEnqueued` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `TitleSaved` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `TitleUnchanged` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/ChangeGroupTitleWorkflow.kt` |
| `AdminAuthorized` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `AvatarLoaded` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `AvatarPacketsCreated` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `AvatarPacketsEnqueued` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `AvatarSaved` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `CreateAvatarPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `EnqueueAvatarPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `LoadAvatar` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `PublishGroupAvatarWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `SaveAvatar` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/PublishGroupAvatarWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/RemoveGroupAvatarWorkflow.kt` |
| `PublishRemoval` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/RemoveGroupAvatarWorkflow.kt` |
| `RemovalPublished` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/RemoveGroupAvatarWorkflow.kt` |
| `RemoveGroupAvatarWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/RemoveGroupAvatarWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/RemoveGroupAvatarWorkflow.kt` |
| `ItemHandled` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/SendCurrentGroupMetadataWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/SendCurrentGroupMetadataWorkflow.kt` |
| `SendAvatar` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/SendCurrentGroupMetadataWorkflow.kt` |
| `SendCurrentGroupMetadataWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/SendCurrentGroupMetadataWorkflow.kt` |
| `SendDescription` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/SendCurrentGroupMetadataWorkflow.kt` |
| `SendExpenses` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/SendCurrentGroupMetadataWorkflow.kt` |
| `SendPin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/SendCurrentGroupMetadataWorkflow.kt` |
| `SendTitle` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/SendCurrentGroupMetadataWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/metadata/SendCurrentGroupMetadataWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/payment/SendGroupPaymentWorkflow.kt` |
| `SendGroupPaymentWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/payment/SendGroupPaymentWorkflow.kt` |
| `SendPayment` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/payment/SendGroupPaymentWorkflow.kt` |
| `Sent` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/payment/SendGroupPaymentWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/payment/SendGroupPaymentWorkflow.kt` |
| `ValidatePayment` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/payment/SendGroupPaymentWorkflow.kt` |
| `Validated` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/payment/SendGroupPaymentWorkflow.kt` |
| `AdminAuthorized` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `BoardClosedMessageSent` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `CloseGroupExpensesWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `CreatePinPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `DeleteReceipts` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `EmptyPinSaved` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `EnqueuePinPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `ExpenseBoardFound` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `FindPin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `NoActiveExpenseBoard` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `NothingPinned` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `PinPacketsCreated` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `PinPacketsEnqueued` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `ReceiptsDeleted` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `SaveEmptyPin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `SendBoardClosedMessage` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/CloseGroupExpensesWorkflow.kt` |
| `AdminAuthorized` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `AlreadyPinned` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `CheckPinSlot` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `CreatePinPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `EnqueuePinPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `FindPinTarget` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `PinGroupMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `PinPacketsCreated` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `PinPacketsEnqueued` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `PinSaved` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `ResolveSenderKey` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `SavePin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `SenderKeyResolved` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `SlotFree` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `SlotOccupiedByExpenses` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `TargetAlreadyPinned` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `TargetFound` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/PinGroupMessageWorkflow.kt` |
| `CreatePinPacket` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `EnqueuePinPacket` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `FindPin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `NoPin` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `PinFound` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `PinPacketCreated` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `PinPacketEnqueued` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `SendCurrentGroupPinWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/SendCurrentGroupPinWorkflow.kt` |
| `AdminAuthorized` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `CreatePinPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `EmptyPinSaved` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `EnqueuePinPackets` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `FindPin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `NothingPinned` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `PinFound` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `PinNotRemovable` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `PinPacketsCreated` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `PinPacketsEnqueued` | object | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `SaveEmptyPin` | data class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |
| `UnpinGroupMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/groupstate/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/groupstate/domain/workflow/pin/UnpinGroupMessageWorkflow.kt` |

### `:feature:conversationorchestration:membership`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `GroupConversationDataSource` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/datasource/GroupConversationDataSource.kt` |
| `GroupIncomingConversationDataSource` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/datasource/GroupIncomingConversationDataSource.kt` |
| `GroupLocalCleanupDataSource` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/datasource/GroupLocalCleanupDataSource.kt` |
| `GroupVerificationDataSource` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/datasource/GroupVerificationDataSource.kt` |
| `GroupSystemMessageFactory` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/mapper/GroupSystemMessageFactory.kt` |
| `GroupSystemMessageDto` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/model/GroupSystemMessageDto.kt` |
| `PreviousGroupMembershipDto` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/model/PreviousGroupMembershipDto.kt` |
| `GroupVerificationPacketCodecImpl` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/packet/GroupVerificationPacketCodecImpl.kt` |
| `GroupVerificationPayloadEncoder` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/packet/GroupVerificationPayloadEncoder.kt` |
| `GroupConversationRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/repository/GroupConversationRepositoryImpl.kt` |
| `GroupIncomingConversationRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/repository/GroupIncomingConversationRepositoryImpl.kt` |
| `GroupVerificationRepositoryImpl` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/repository/GroupVerificationRepositoryImpl.kt` |
| `HandleGroupInvitationResult` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/command/MembershipCommand.kt` |
| `HandleMembershipResult` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/command/MembershipCommand.kt` |
| `MembershipCommand` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/command/MembershipCommand.kt` |
| `RecoverAcceptedGroupInvitation` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/command/MembershipCommand.kt` |
| `RefreshOwnedGroupVerification` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/command/MembershipCommand.kt` |
| `ResumeApprovedGroupJoin` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/command/MembershipCommand.kt` |
| `GroupParticipantActivation` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/model/GroupParticipantActivation.kt` |
| `GroupParticipantAddition` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/model/GroupParticipantAddition.kt` |
| `GroupParticipantRemoval` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/model/GroupParticipantRemoval.kt` |
| `GroupVerificationRow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/model/GroupVerificationRow.kt` |
| `GroupVerificationStatus` | enum class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/model/GroupVerificationRow.kt` |
| `GroupVerificationSigner` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/model/GroupVerificationSigner.kt` |
| `OwnedGroupVerificationInputs` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/model/OwnedGroupVerificationInputs.kt` |
| `PendingGroupIdentity` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/model/PendingGroupIdentity.kt` |
| `GroupVerificationPacketCodec` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/packet/GroupVerificationPacketCodec.kt` |
| `GroupVerificationPacketPolicy` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/policy/GroupVerificationPacketPolicy.kt` |
| `GroupConversationRepository` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/repository/GroupConversationRepository.kt` |
| `GroupIncomingConversationRepository` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/repository/GroupIncomingConversationRepository.kt` |
| `GroupVerificationRepository` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/repository/GroupVerificationRepository.kt` |
| `AcceptParticipantVerificationReceiptUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/AcceptParticipantVerificationReceiptUseCase.kt` |
| `AcceptVerificationSnapshotRequestUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/AcceptVerificationSnapshotRequestUseCase.kt` |
| `ApplyIncomingGroupRemovalUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/ApplyIncomingGroupRemovalUseCase.kt` |
| `ApplyVerificationSnapshotUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/ApplyVerificationSnapshotUseCase.kt` |
| `BroadcastGroupVerificationSnapshotUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/BroadcastGroupVerificationSnapshotUseCase.kt` |
| `CheckGroupOwnershipUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/CheckGroupOwnershipUseCase.kt` |
| `ConfirmGroupMemberIdentityUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/ConfirmGroupMemberIdentityUseCase.kt` |
| `ConfirmParticipantVerificationUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/ConfirmParticipantVerificationUseCase.kt` |
| `FindMemberDisplayNameUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/FindMemberDisplayNameUseCase.kt` |
| `GroupVerificationInputsUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/GroupVerificationInputsUseCase.kt` |
| `LoadGroupVerificationSignerUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/LoadGroupVerificationSignerUseCase.kt` |
| `RecordGroupInvitationUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/RecordGroupInvitationUseCase.kt` |
| `RefreshOwnedGroupVerificationUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/RefreshOwnedGroupVerificationUseCase.kt` |
| `RequestGroupVerificationSnapshotUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/RequestGroupVerificationSnapshotUseCase.kt` |
| `RequireMemberPhoneNumberUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/RequireMemberPhoneNumberUseCase.kt` |
| `ResolveSigningIdentityContactUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/ResolveSigningIdentityContactUseCase.kt` |
| `SendAdminVerificationReceiptUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/SendAdminVerificationReceiptUseCase.kt` |
| `SendGroupVerificationSnapshotUseCase` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/usecase/SendGroupVerificationSnapshotUseCase.kt` |
| `AddGroupMembersWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `InvitationRecorded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `LoadTitle` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `MembershipStarted` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `RecordInvitation` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `RefreshVerification` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `StartMembership` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `TitleLoaded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `VerificationRefreshed` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/AddGroupMembersWorkflow.kt` |
| `ConversationCreated` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `CreateConversation` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `CreateGroupWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `InitializeSecurity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `InitializeVerification` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `InviteMembers` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `MembersInvited` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `SecurityInitialized` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `VerificationInitialized` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/CreateGroupWorkflow.kt` |
| `AttachmentsDeleted` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `ClearGroupState` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `ContextLoaded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `DeleteAttachments` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `DeleteGroupWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `DeleteLocalConversation` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `DeleteMembership` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `GroupStateCleared` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `LoadContext` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `LocalConversationDeleted` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `MembershipDeleted` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/DeleteGroupWorkflow.kt` |
| `ContextLoaded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `DeleteExpenseReceipts` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `EndLocalMembership` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `ExpenseReceiptsDeleted` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `LeaveGroup` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `LeaveGroupWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `LoadContext` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `LocalMembershipEnded` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `MembershipEnded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/LeaveGroupWorkflow.kt` |
| `ContextLoaded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `LoadContext` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `MemberPromoted` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `ProjectPromotion` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `PromoteGroupMemberWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `PromoteMember` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `PromotionProjected` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `RefreshVerification` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `VerificationRefreshed` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/PromoteGroupMemberWorkflow.kt` |
| `ContextLoaded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `LoadContext` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `MemberNameResolved` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `MemberRemoved` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `ProjectRemoval` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `RefreshVerification` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `RemovalProjected` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `RemoveGroupMemberWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `RemoveMember` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `ResolveMemberName` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `VerificationRefreshed` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/RemoveGroupMemberWorkflow.kt` |
| `ContextLoaded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `DeleteExpenseReceipts` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `EndLocalMembership` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `ExpenseReceiptsDeleted` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `LoadContext` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `LocalMembershipEnded` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `MembershipEnded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `TransferAdminAndLeave` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `TransferGroupAdminAndLeaveWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/administration/TransferGroupAdminAndLeaveWorkflow.kt` |
| `AcceptMembership` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `AcceptOwnerIdentity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `AcceptedHandshakeFound` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `DeclineMembership` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `DeclinedHandshakeFound` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `DiscardConversation` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `FindHandshake` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `HandleGroupInvitationResultWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `MembershipAccepted` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `MembershipDeclined` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `NotForThisDevice` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `OwnerIdentityAccepted` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `OwnerIdentityStaged` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `OwnerKeys` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `ShowConversation` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `StageOwnerIdentity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleGroupInvitationResultWorkflow.kt` |
| `AddParticipant` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `ConfirmMemberIdentity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `EpochLoaded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `FlushQueuedMessages` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `HandleMembershipResultWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `IgnoreResult` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `InvitationAccepted` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `LoadEpoch` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `LoadMemberIdentity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `MarkInvitationAccepted` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `MemberIdentityLoaded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `MemberNameResolved` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `ParticipantAdded` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `ResolveMemberName` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/HandleMembershipResultWorkflow.kt` |
| `CheckHandshakeExists` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/RecoverAcceptedGroupInvitationWorkflow.kt` |
| `HandleResult` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/RecoverAcceptedGroupInvitationWorkflow.kt` |
| `HandshakeExists` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/RecoverAcceptedGroupInvitationWorkflow.kt` |
| `NothingToRecover` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/RecoverAcceptedGroupInvitationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/RecoverAcceptedGroupInvitationWorkflow.kt` |
| `RecoverAcceptedGroupInvitationWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/RecoverAcceptedGroupInvitationWorkflow.kt` |
| `ResultHandled` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/RecoverAcceptedGroupInvitationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/RecoverAcceptedGroupInvitationWorkflow.kt` |
| `ConfirmMemberIdentity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `EstablishMutualIdentity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `FindHandshake` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `HandshakeFound` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `IdentityLoaded` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `IdentityStaged` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `LoadApprovedIdentity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `MemberIdentityConfirmed` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `MutualIdentityEstablished` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `ResumeApprovedGroupJoinWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `StageIdentity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/join/ResumeApprovedGroupJoinWorkflow.kt` |
| `BroadcastSnapshot` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `CheckOwnership` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `LoadSecurity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `LocalAdmin` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `NoSecurityState` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `OwnershipConfirmed` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `RefreshOwnedGroupVerificationWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `RefreshRows` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `RowsRefreshed` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `SnapshotBroadcast` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/RefreshOwnedGroupVerificationWorkflow.kt` |
| `BroadcastSnapshot` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `CheckOwnership` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `LoadSecurity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `LocalAdmin` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `Member` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `NoSecurityState` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `OwnershipChecked` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `RefreshRows` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `RequestSnapshot` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `RowsRefreshed` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `SnapshotBroadcast` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `SnapshotRequested` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `SynchronizeGroupVerificationWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/SynchronizeGroupVerificationWorkflow.kt` |
| `BroadcastSnapshot` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `ConfirmParticipant` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `LoadSecurity` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `LocalAdmin` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `ParticipantConfirmed` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `ReceiptSent` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `SendReceipt` | data class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `SnapshotBroadcast` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `TargetIsAdmin` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `TargetIsNotAdmin` | object | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `VerifyGroupMemberWorkflow` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/domain/workflow/verification/VerifyGroupMemberWorkflow.kt` |
| `MembershipResultObserver` | class | `commonMain` | `feature/conversationorchestration/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/runtime/MembershipResultObserver.kt` |
| `GroupSystemMessageFactoryTest` | class | `commonTest` | `feature/conversationorchestration/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/membership/data/mapper/GroupSystemMessageFactoryTest.kt` |

### `:feature:conversationorchestration:lifecycle`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AcceptApprovedIdentityChange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `HandleContactBlocked` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `HandleDirectInvitationResult` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `HandleIdentityResult` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `LifecycleCommand` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `RecordIncomingDirectInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `RecoverAcceptedDirectInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `RecoverAuthorizedConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `RequestReauthorization` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `RevokePeerExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `StartDirectInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `StartRecoveryInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/command/LifecycleCommand.kt` |
| `ApplyPeerProfilePictureUseCase` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/usecase/ApplyPeerProfilePictureUseCase.kt` |
| `CheckIncomingInvitationAllowedUseCase` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/usecase/CheckIncomingInvitationAllowedUseCase.kt` |
| `RecordIncomingDirectInvitationUseCase` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/usecase/RecordIncomingDirectInvitationUseCase.kt` |
| `RecordOutgoingDirectInvitationUseCase` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/usecase/RecordOutgoingDirectInvitationUseCase.kt` |
| `ContactFound` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `ConversationAlreadyGone` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `ConversationDeleted` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `DeleteConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `DeleteDirectConversationWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `ExchangeRevoked` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `FindContact` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `MailboxRevoked` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `RevokeExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `RevokeMailbox` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/DeleteDirectConversationWorkflow.kt` |
| `Authorized` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `CheckAuthorization` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `ConversationFound` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `ConversationMissing` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `InvitationStarted` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `NotAuthorized` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `OpenDirectConversationWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `StartInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/OpenDirectConversationWorkflow.kt` |
| `Authorized` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `CheckReconnectable` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `ExchangeRevoked` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `InvitationStarted` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `LoadAuthorization` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `NotAuthorized` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `PreflightRouting` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `ReconnectDirectConversationWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `Reconnectable` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `RevokeExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `RoutingAvailable` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `StartInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/ReconnectDirectConversationWorkflow.kt` |
| `Authorized` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `ConversationExists` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `ConversationMissing` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `InvitationStarted` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `LoadAuthorization` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `NotAuthorized` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `Reconnect` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `RequestReauthorizationWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `StartInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RequestReauthorizationWorkflow.kt` |
| `CloseExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `DeclineExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `ExchangeClosed` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `ExchangeEnded` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `ExchangeFound` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `FindExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `NoExchange` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `RevokePeerExchangeWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `SendRevocation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/RevokePeerExchangeWorkflow.kt` |
| `Authorized` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `CheckNotBlocked` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `ConversationExists` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `ConversationMissing` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `ExchangeRevoked` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `ExchangeStarted` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `InvitationRecorded` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `LabelsLoaded` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `LoadAuthorization` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `LoadLabels` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `NotAuthorized` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `NotBlocked` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `PreflightRouting` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `RecordInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `RevokeExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `RoutingAvailable` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `StartDirectInvitationWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `StartExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/direct/StartDirectInvitationWorkflow.kt` |
| `AcceptApprovedIdentityChangeWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `AcceptOriginalInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `ApprovalValid` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `CheckPinnedIdentity` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `InvalidateStaleInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `OriginalInvitationAccepted` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `PinnedIdentityMatches` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `StaleInvitationInvalidated` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `ValidateApproval` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/AcceptApprovedIdentityChangeWorkflow.kt` |
| `CheckExchangeStillOpen` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAcceptedDirectInvitationWorkflow.kt` |
| `ExchangeStillOpen` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAcceptedDirectInvitationWorkflow.kt` |
| `HandleResult` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAcceptedDirectInvitationWorkflow.kt` |
| `NothingToRecover` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAcceptedDirectInvitationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAcceptedDirectInvitationWorkflow.kt` |
| `RecoverAcceptedDirectInvitationWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAcceptedDirectInvitationWorkflow.kt` |
| `ResultHandled` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAcceptedDirectInvitationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAcceptedDirectInvitationWorkflow.kt` |
| `ActivateConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `Authorized` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `CheckExchangeStillActive` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `ConversationActivated` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `ConversationExists` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `ConversationMissing` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `ExchangeStillActive` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `LoadAuthorization` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `NothingToRecover` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `RecoverAuthorizedConversationWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/RecoverAuthorizedConversationWorkflow.kt` |
| `AcceptOriginalInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `DirectApproval` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `EnsureConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `FindGroupMembership` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `MemberSideGroupOffer` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `OriginalInvitationUnusable` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `OriginalInvitationValid` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `OwnerSideGroupJoin` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `RequestReauthorization` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `ResumeGroupJoin` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `StartRecoveryInvitationWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `WithdrawnGroupMembership` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/recovery/StartRecoveryInvitationWorkflow.kt` |
| `CheckStillBlocked` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleContactBlockedWorkflow.kt` |
| `ExchangeRevoked` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleContactBlockedWorkflow.kt` |
| `HandleContactBlockedWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleContactBlockedWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleContactBlockedWorkflow.kt` |
| `RevokeExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleContactBlockedWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleContactBlockedWorkflow.kt` |
| `StillBlocked` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleContactBlockedWorkflow.kt` |
| `Unblocked` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleContactBlockedWorkflow.kt` |
| `AcceptExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `ActivateConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `BlockContact` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `DeclineExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `DiscardWaitingMessages` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `ExchangeAccepted` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `ExchangeDeclined` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `HandleDirectInvitationResultWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `IgnoreResult` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleDirectInvitationResultWorkflow.kt` |
| `ActivateConversation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `ConversationActivated` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `DiscardWaitingMessages` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `DisplayNameResolved` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `HandleIdentityResultWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `InvalidateInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `InvitationAccepted` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `InvitationDeclined` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `LoadSetupMode` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `MarkInvitationAccepted` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `MarkInvitationDeclined` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `MarkTransportFailed` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `RecordIncomingInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `ResolveDisplayName` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `SendVerificationReceipt` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `SetupModeLoaded` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/HandleIdentityResultWorkflow.kt` |
| `Allowed` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `CheckAllowed` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `DeclineExchange` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `ExchangeDeclined` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `InvitationRecorded` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `RecordIncomingDirectInvitationWorkflow` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `RecordInvitation` | data class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `Rejected` | object | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/domain/workflow/result/RecordIncomingDirectInvitationWorkflow.kt` |
| `ApprovedIdentityReconnectionObserver` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/runtime/ApprovedIdentityReconnectionObserver.kt` |
| `ApprovedReconnectionRetryWorker` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/runtime/ApprovedReconnectionRetryWorker.kt` |
| `ContactBlockObserver` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/runtime/ContactBlockObserver.kt` |
| `IdentityResultObserver` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/runtime/IdentityResultObserver.kt` |
| `InvitationResultObserver` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/runtime/InvitationResultObserver.kt` |
| `InvitationTransportFailureHandler` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/runtime/InvitationTransportFailureHandler.kt` |
| `MessagingTransportResultObserver` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/runtime/MessagingTransportResultObserver.kt` |
| `WorkflowDirectReauthorization` | class | `commonMain` | `feature/conversationorchestration/lifecycle/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/runtime/WorkflowDirectReauthorization.kt` |
| `ApprovedReconnectionRetryWorkerTest` | class | `commonTest` | `feature/conversationorchestration/lifecycle/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/lifecycle/runtime/ApprovedReconnectionRetryWorkerTest.kt` |

### `:feature:conversationorchestration:incoming`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `DirectMessagePacketReaderImpl` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/data/packet/DirectMessagePacketReaderImpl.kt` |
| `ApplyRemoteMessageOperation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `CompleteManualIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `IncomingCommand` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveAuthorizationRevoked` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveContactInvite` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveContactInviteAccepted` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveContactInviteDeclined` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveContactReady` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveDirectMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupActivationAcknowledgement` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupAvatar` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupDeletion` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupDescription` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupInvite` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupInviteDecline` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupInviteReceipt` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupJoinRequest` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupLeaveRequest` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupMemberActivation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupMemberRemoval` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupPin` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupReadyAcknowledgement` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupTitle` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveGroupWelcome` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveIdentityAcknowledgement` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveManualIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveVerificationReceipt` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveVerificationSnapshot` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ReceiveVerificationSnapshotRequest` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `RouteIncomingPacket` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/command/IncomingCommand.kt` |
| `ControlPacketDispatcher` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/dispatch/ControlPacketDispatcher.kt` |
| `GroupPacketDispatcher` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/dispatch/GroupPacketDispatcher.kt` |
| `RemoteIdentityReplacementRequiredException` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/error/RemoteIdentityReplacementRequiredException.kt` |
| `IncomingMessageContent` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/model/IncomingMessageContent.kt` |
| `OperationConversation` | enum class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/model/OperationConversation.kt` |
| `ReceivedPacket` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/model/ReceivedPacket.kt` |
| `DirectMessagePacketReader` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/packet/DirectMessagePacketReader.kt` |
| `GroupInvitationIdentityDisposition` | enum class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/policy/GroupInvitationIdentityPolicy.kt` |
| `GroupInvitationIdentityPolicy` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/policy/GroupInvitationIdentityPolicy.kt` |
| `IncomingAuthorizationRevocationDecision` | enum class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/policy/IncomingAuthorizationRevocationDecision.kt` |
| `IncomingExpensePolicy` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/policy/IncomingExpensePolicy.kt` |
| `AuthorizeIncomingExpensesUseCase` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/AuthorizeIncomingExpensesUseCase.kt` |
| `CheckGroupPacketAcceptedUseCase` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/CheckGroupPacketAcceptedUseCase.kt` |
| `DeleteReplacedExpenseReceiptsUseCase` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/DeleteReplacedExpenseReceiptsUseCase.kt` |
| `FindOperationTargetUseCase` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/FindOperationTargetUseCase.kt` |
| `ResolveIncomingIdentityPeerUseCase` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/ResolveIncomingIdentityPeerUseCase.kt` |
| `ResolveSenderDisplayNameUseCase` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/ResolveSenderDisplayNameUseCase.kt` |
| `SendDeliveryReceiptUseCase` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/SendDeliveryReceiptUseCase.kt` |
| `StoreUnreadableMessageUseCase` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/StoreUnreadableMessageUseCase.kt` |
| `AttachmentsCached` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `AttachmentsStored` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `AutoReplyClaimed` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `AutoReplyFailed` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `AutoReplyHandled` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `CacheAttachments` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `ClaimAutoReply` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `DeliveryReceiptSent` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `MessageRead` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `MessageStored` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `NoAutoReply` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `ReadMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `ReceiveDirectMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `ReleaseAutoReplyClaim` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `SendAutoReply` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `SendDeliveryReceipt` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `SenderProfileUpdated` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `StoreAttachments` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `StoreMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `UpdateSenderProfile` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/direct/ReceiveDirectMessageWorkflow.kt` |
| `ApplyOperation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `ApplyProfilePicture` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `AttachmentsStored` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `AuthorizeExpenses` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `CacheAttachments` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `ConversationFound` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `DecryptMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `DeleteReplacedReceipts` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `DeliveryReceiptSent` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `ExpensesAuthorized` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `FindConversation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `FindExisting` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `LinkPreviewsRequested` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `MessageDecrypted` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `MessageStored` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `NewMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `OperationDecrypted` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `PrefetchLinkPreviews` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `ProfilePictureApplied` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `ReceiveGroupMessageWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `ReceivedGroupMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `ReplacedReceiptsDeleted` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `SendDeliveryReceipt` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `StoreAttachments` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `StoreMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/group/ReceiveGroupMessageWorkflow.kt` |
| `ActivateConversation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `Authorized` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `CompleteManualIdentityWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `ConversationActivated` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `LoadAuthorization` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `NotAuthorized` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `ReceiptSent` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `SendVerificationReceipt` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/CompleteManualIdentityWorkflow.kt` |
| `DecideRevocation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `ExchangeFound` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `FindExchange` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `InvalidateExchange` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `ReceiveAuthorizationRevokedWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `RevocationApplies` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `SignatureValid` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `VerifySignature` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveAuthorizationRevokedWorkflow.kt` |
| `AcceptanceReceived` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `AcceptanceVerified` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `ActivateConversation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `ApplyProfilePicture` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `ConversationActivated` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `ExchangeFound` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `FindExchange` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `InvitationAccepted` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `MarkInvitationAccepted` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `ReceiveAcceptance` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `ReceiveContactInviteAcceptedWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `ReviewRequired` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `StageIdentityReview` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `VerifyAcceptance` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteAcceptedWorkflow.kt` |
| `DeclineRecorded` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteDeclinedWorkflow.kt` |
| `DeclineVerified` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteDeclinedWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteDeclinedWorkflow.kt` |
| `ReceiveContactInviteDeclinedWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteDeclinedWorkflow.kt` |
| `RecordDecline` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteDeclinedWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteDeclinedWorkflow.kt` |
| `VerifyDecline` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteDeclinedWorkflow.kt` |
| `ApplyProfilePicture` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `DisplayNameResolved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `ExchangeReceived` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `InvitationVerified` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `MergePeers` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `PeerResolved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `PeerUpdated` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `PeersMerged` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `ProfilePictureApplied` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `ReceiveContactInviteWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `ReceiveExchange` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `RecordInvitation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `ReplacementRequired` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `ResolveDisplayName` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `ResolvePeer` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `StageReplacementReview` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `UpdatePeerPhoneNumber` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `VerifyInvitation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactInviteWorkflow.kt` |
| `ExchangeFound` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactReadyWorkflow.kt` |
| `FindExchange` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactReadyWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactReadyWorkflow.kt` |
| `ReadyReceived` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactReadyWorkflow.kt` |
| `ReadyVerified` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactReadyWorkflow.kt` |
| `ReceiveContactReadyWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactReadyWorkflow.kt` |
| `ReceiveReady` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactReadyWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactReadyWorkflow.kt` |
| `VerifyReady` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveContactReadyWorkflow.kt` |
| `CompleteIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveIdentityAcknowledgementWorkflow.kt` |
| `IdentityCompleted` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveIdentityAcknowledgementWorkflow.kt` |
| `IdentityMutual` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveIdentityAcknowledgementWorkflow.kt` |
| `IdentityOneWay` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveIdentityAcknowledgementWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveIdentityAcknowledgementWorkflow.kt` |
| `Receive` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveIdentityAcknowledgementWorkflow.kt` |
| `ReceiveIdentityAcknowledgementWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveIdentityAcknowledgementWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveIdentityAcknowledgementWorkflow.kt` |
| `CompleteIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveManualIdentityWorkflow.kt` |
| `IdentityCompleted` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveManualIdentityWorkflow.kt` |
| `IdentityMutual` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveManualIdentityWorkflow.kt` |
| `IdentityOneWay` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveManualIdentityWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveManualIdentityWorkflow.kt` |
| `Receive` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveManualIdentityWorkflow.kt` |
| `ReceiveManualIdentityWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveManualIdentityWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/identity/ReceiveManualIdentityWorkflow.kt` |
| `AcknowledgementReceived` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupActivationAcknowledgementWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupActivationAcknowledgementWorkflow.kt` |
| `ReceiveAcknowledgement` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupActivationAcknowledgementWorkflow.kt` |
| `ReceiveGroupActivationAcknowledgementWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupActivationAcknowledgementWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupActivationAcknowledgementWorkflow.kt` |
| `AttachmentsDeleted` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `AuthorizeDeletion` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `CompleteDeletion` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `DeleteAttachments` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `DeletionAuthorized` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `DeletionPrepared` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `FinishDeletion` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `Finished` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `LoadOwnerKey` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `MembershipDeleted` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `OwnerKeyLoaded` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `PrepareDeletion` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `ReceiveGroupDeletionWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupDeletionWorkflow.kt` |
| `ClearHandshake` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `DeclineReceived` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `HandshakeCleared` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `MarkInvitationDeclined` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `MarkMembershipRemoved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `ReceiveDecline` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `ReceiveGroupInviteDeclineWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `RequireSigningIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `SigningIdentityConfirmed` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteDeclineWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteReceiptWorkflow.kt` |
| `ReceiptReceived` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteReceiptWorkflow.kt` |
| `ReceiveGroupInviteReceiptWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteReceiptWorkflow.kt` |
| `ReceiveReceipt` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteReceiptWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteReceiptWorkflow.kt` |
| `AlreadyHandled` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `ApplyProfilePicture` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `CheckPinnedOwner` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `CheckRecordable` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `ConversationDeletedLocally` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `ConversationStaged` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `Dropped` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `EvaluateOwnerIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `IdentityReviewStaged` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `InspectOffer` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `OfferStaged` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `OfferValid` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `OwnerIdentityEvaluated` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `OwnerIdentityStaged` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `OwnerKeysTrusted` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `ReceiveGroupInviteWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `ReceiveWithoutStaging` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `ReceivedWithoutStaging` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `RecordInvitation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `Recordable` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `StageConversation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `StageIdentityReview` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `StageOffer` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `StageOwnerIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupInviteWorkflow.kt` |
| `ApplyProfilePicture` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `ConfirmMemberIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `EstablishMutualIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `EvaluateMemberIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `IdentityAcceptable` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `IdentityChanged` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `IdentityStaged` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `JoinReceived` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `MemberIdentityConfirmed` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `MutualIdentityEstablished` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `ReceiveGroupJoinRequestWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `ReceiveJoin` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `StageIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `StageIdentityReview` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupJoinRequestWorkflow.kt` |
| `ContextLoaded` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `LoadContext` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `MemberLeft` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `MemberNameResolved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `ParticipantRemoved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `ReceiveGroupLeaveRequestWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `ReceiveLeaveRequest` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `RefreshVerification` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `RemoveParticipant` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `ResolveMemberName` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `VerificationRefreshed` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupLeaveRequestWorkflow.kt` |
| `ActivateParticipant` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `AppliedForLocalMember` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `AppliedForMember` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `ApplyActivation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `AuthorizeActivation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `Done` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `FlushQueuedMessages` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `LoadOwner` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `LocalMemberActivated` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `MemberContactResolved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `MemberNameResolved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `OwnerLoaded` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `ParticipantActivated` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `ReceiveGroupMemberActivationWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `RecordMemberAdded` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `RemoteMemberActivated` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `ResolveMemberContact` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `ResolveMemberName` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `SynchronizeVerification` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberActivationWorkflow.kt` |
| `ApplyRemoval` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `AuthorizeRemoval` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `CompleteRemoval` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `Completed` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `NotApplicable` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `ReceiveGroupMemberRemovalWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `RemovalApplied` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `RemovalAuthorized` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupMemberRemovalWorkflow.kt` |
| `AcknowledgementReceived` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `AddParticipant` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `CheckEpochCurrent` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `CheckEpochStillCurrent` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `EpochCurrent` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `EpochStillCurrent` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `FindActivatedMembership` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `FlushQueuedMessages` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `MemberNameResolved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `MembershipActivated` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `MetadataSent` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `NoActivatedMembership` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `Obsolete` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `ParticipantAdded` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `QueueFlushed` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `ReceiveAcknowledgement` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `ReceiveGroupReadyAcknowledgementWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `RefreshVerification` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `ResolveMemberName` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `SendCurrentMetadata` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `VerificationRefreshed` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupReadyAcknowledgementWorkflow.kt` |
| `Advanced` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `AuthorizeWelcome` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `CompleteWelcome` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `Completed` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `EstablishMutualIdentity` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `InstallWelcome` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `LoadAuthority` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `NotApplicable` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `OpenWelcome` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `PersistWelcome` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `ReceiveGroupWelcomeWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `RecordRestart` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `ResolveMemberContacts` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `ResolveMemberNames` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `SendReadyAcknowledgement` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `Welcome` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/membership/ReceiveGroupWelcomeWorkflow.kt` |
| `AdminAuthorized` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `Ignored` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `LoadAvatar` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `ReceiveGroupAvatarWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `SaveAvatar` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `Saved` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `SignatureValid` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `UpdateIsNewer` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `VerifySignature` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupAvatarWorkflow.kt` |
| `AdminAuthorized` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `Ignored` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `LoadDescription` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `ReceiveGroupDescriptionWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `SaveDescription` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `Saved` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `SignatureValid` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `UpdateIsNewer` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `VerifySignature` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupDescriptionWorkflow.kt` |
| `AdminAuthorized` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `CheckPinSlot` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `Ignored` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `ReceiveGroupPinWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `ResolveMessageSender` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `SavePin` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `Saved` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `SenderResolved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `SignatureValid` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `SlotAvailable` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `VerifySignature` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupPinWorkflow.kt` |
| `AdminAuthorized` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `AuthorizeAdmin` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `Ignored` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `LoadTitle` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `ReceiveGroupTitleWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `SaveTitle` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `Saved` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `SignatureValid` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `UpdateIsNewer` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `VerifySignature` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/metadata/ReceiveGroupTitleWorkflow.kt` |
| `Applied` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `ApplyRemoteMessageOperationWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `CheckEditable` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `CheckPollCloser` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `CheckSender` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `DeleteMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `Editable` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `FindPoll` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `FindTarget` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `PollCloserAllowed` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `PollFound` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `RemoveReaction` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `ReplaceText` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `SavePoll` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `SaveReaction` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `SenderOwnsTarget` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `TargetFound` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `TargetMissing` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/operation/ApplyRemoteMessageOperationWorkflow.kt` |
| `ApplyDirectOperation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `ApplyReceipt` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `CheckDirectAuthorization` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `CheckGroupAccepted` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `ControlConversationResolved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `DirectAuthorized` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `DirectConversationResolved` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `DispatchControlPacket` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `DispatchGroupPacket` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `Dropped` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `EnsureDirectConversation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `FindControlConversation` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `GroupAccepted` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `Handled` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `ReceiveDirectMessage` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `RouteIncomingPacketWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/routing/RouteIncomingPacketWorkflow.kt` |
| `AcceptReceipt` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationReceiptWorkflow.kt` |
| `BroadcastSnapshot` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationReceiptWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationReceiptWorkflow.kt` |
| `ReceiptAccepted` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationReceiptWorkflow.kt` |
| `ReceiveVerificationReceiptWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationReceiptWorkflow.kt` |
| `SnapshotBroadcast` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationReceiptWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationReceiptWorkflow.kt` |
| `AuthenticateRequest` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotRequestWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotRequestWorkflow.kt` |
| `ReceiveVerificationSnapshotRequestWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotRequestWorkflow.kt` |
| `RefreshRows` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotRequestWorkflow.kt` |
| `RequestAuthenticated` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotRequestWorkflow.kt` |
| `RowsRefreshed` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotRequestWorkflow.kt` |
| `SendSnapshot` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotRequestWorkflow.kt` |
| `SnapshotSent` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotRequestWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotRequestWorkflow.kt` |
| `ApplySnapshot` | data class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotWorkflow.kt` |
| `Outcome` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotWorkflow.kt` |
| `ReceiveVerificationSnapshotWorkflow` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotWorkflow.kt` |
| `SnapshotApplied` | object | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotWorkflow.kt` |
| `Step` | interface | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/workflow/verification/ReceiveVerificationSnapshotWorkflow.kt` |
| `IncomingEnvelopeProcessorImpl` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/runtime/IncomingEnvelopeProcessorImpl.kt` |
| `IncomingTransportMessageHandler` | class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/runtime/IncomingTransportMessageHandler.kt` |
| `Unreadable` | enum class | `commonMain` | `feature/conversationorchestration/incoming/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/runtime/IncomingTransportMessageHandler.kt` |
| `GroupInvitationIdentityPolicyTest` | class | `commonTest` | `feature/conversationorchestration/incoming/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/policy/GroupInvitationIdentityPolicyTest.kt` |
| `IncomingAuthorizationRevocationDecisionTest` | class | `commonTest` | `feature/conversationorchestration/incoming/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/policy/IncomingAuthorizationRevocationDecisionTest.kt` |
| `IncomingExpensePolicyTest` | class | `commonTest` | `feature/conversationorchestration/incoming/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/policy/IncomingExpensePolicyTest.kt` |
| `FakeContacts` | class | `commonTest` | `feature/conversationorchestration/incoming/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/ResolveIncomingIdentityPeerUseCaseTest.kt` |
| `FakeIdentities` | class | `commonTest` | `feature/conversationorchestration/incoming/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/ResolveIncomingIdentityPeerUseCaseTest.kt` |
| `ResolveIncomingIdentityPeerUseCaseTest` | class | `commonTest` | `feature/conversationorchestration/incoming/src/commonTest/kotlin/com/cbgm/sparrow/feature/conversationorchestration/incoming/domain/usecase/ResolveIncomingIdentityPeerUseCaseTest.kt` |

### `:feature:membership`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `SodiumGroupCryptoTest` | class | `androidDeviceTest` | `feature/membership/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/membership/crypto/SodiumGroupCryptoTest.kt` |
| `TestIdentityKeys` | class | `androidDeviceTest` | `feature/membership/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/membership/crypto/SodiumGroupCryptoTest.kt` |
| `AndroidGroupKeyStoreTest` | class | `androidDeviceTest` | `feature/membership/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/membership/device/AndroidGroupKeyStoreTest.kt` |
| `AndroidGroupKeyStore` | class | `androidMain` | `feature/membership/src/androidMain/kotlin/com/cbgm/sparrow/feature/membership/device/AndroidGroupKeyStore.kt` |
| `GroupCiphertext` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/crypto/GroupCiphertext.kt` |
| `GroupCrypto` | interface | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/crypto/GroupCrypto.kt` |
| `GroupKeyConfirmation` | object | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/crypto/GroupKeyConfirmation.kt` |
| `GroupKeyStore` | interface | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/crypto/GroupKeyStore.kt` |
| `SodiumGroupCrypto` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/crypto/SodiumGroupCrypto.kt` |
| `GroupMembershipLock` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipLock.kt` |
| `GroupMembershipEvent` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipStateMachine.kt` |
| `GroupMembershipStateMachine` | object | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipStateMachine.kt` |
| `GroupEpochDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupEpochDataSource.kt` |
| `MemberIdentityDto` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupEpochDataSource.kt` |
| `GroupEpochSecurityDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupEpochSecurityDataSource.kt` |
| `GroupIncomingActivationDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingActivationDataSource.kt` |
| `GroupIncomingDeletionDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingDeletionDataSource.kt` |
| `GroupIncomingRemovalDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingRemovalDataSource.kt` |
| `GroupIncomingWelcomeDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingWelcomeDataSource.kt` |
| `GroupLeaveDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupLeaveDataSource.kt` |
| `GroupMemberPromotionDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMemberPromotionDataSource.kt` |
| `GroupMemberRemovalDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMemberRemovalDataSource.kt` |
| `MemberRemovalDto` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMemberRemovalDataSource.kt` |
| `GroupMembershipActivationDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipActivationDataSource.kt` |
| `GroupMembershipAdministrationDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipAdministrationDataSource.kt` |
| `GroupMembershipDeletionDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipDeletionDataSource.kt` |
| `GroupMembershipLifecycleDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipLifecycleDataSource.kt` |
| `GroupMembershipStoreDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipStoreDataSource.kt` |
| `GroupOwnerWelcomeDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupOwnerWelcomeDataSource.kt` |
| `GroupPacketBroadcaster` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupPacketBroadcaster.kt` |
| `GroupReadyAcknowledgementDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupReadyAcknowledgementDataSource.kt` |
| `GroupSecurityStoreDataSource` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupSecurityStoreDataSource.kt` |
| `GroupInvitationDirection` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupInvitationStatus.kt` |
| `GroupInvitationStatus` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupInvitationStatus.kt` |
| `GroupLocalMembershipEndDto` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupLocalMembershipEndDto.kt` |
| `CanLeave` | object | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupConversationStateDto` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupLeaveRequirementDto` | interface | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupMemberProgressDto` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupMemberProgressStatusDto` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `PromoteAdminFirst` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupMembershipParticipantDto` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipParticipantDto.kt` |
| `GroupMembershipPeerDto` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipPeerDto.kt` |
| `CreatedGroupSecurityDto` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipSecurityDto.kt` |
| `GroupWelcomeRecipientDto` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipSecurityDto.kt` |
| `GroupMembershipPerspective` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipStatus.kt` |
| `GroupMembershipStatus` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipStatus.kt` |
| `PendingGroupRemovalPolicy` | object | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/policy/PendingGroupRemovalPolicy.kt` |
| `GroupMembershipPacketProtocol` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/protocol/GroupMembershipPacketProtocol.kt` |
| `GroupMembershipPayloadEncoder` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/protocol/GroupMembershipPayloadEncoder.kt` |
| `GroupMembershipRepositoryImpl` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/repository/GroupMembershipRepositoryImpl.kt` |
| `MembershipRepositoryImpl` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/repository/MembershipRepositoryImpl.kt` |
| `GroupSecurityManager` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupSecurityManager.kt` |
| `GroupWelcomeSecurity` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupWelcomeSecurity.kt` |
| `GroupAdministrationState` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupAdministrationState.kt` |
| `GroupConversationMembershipProjection` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipProjection.kt` |
| `GroupConversationMembershipProjector` | object | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipProjection.kt` |
| `GroupConversationMembershipSnapshot` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipSnapshot.kt` |
| `GroupMemberLifecycleSnapshot` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipSnapshot.kt` |
| `GroupConversationState` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationState.kt` |
| `GroupIncomingWelcomeAuthorization` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupIncomingWelcomeAuthorization.kt` |
| `CanLeave` | object | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupLeaveRequirement.kt` |
| `GroupLeaveRequirement` | interface | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupLeaveRequirement.kt` |
| `PromoteAdminFirst` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupLeaveRequirement.kt` |
| `GroupLocalMembershipEnd` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupLocalMembershipEnd.kt` |
| `GroupMemberPromotionResult` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberAdministrationResult.kt` |
| `GroupMemberRemovalReason` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberAdministrationResult.kt` |
| `GroupMemberRemovalResult` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberAdministrationResult.kt` |
| `GroupMemberProgress` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberProgress.kt` |
| `GroupMemberProgressStatus` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberProgress.kt` |
| `GroupMembershipContext` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMembershipContext.kt` |
| `GroupMessageMembershipAccess` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMessageMembershipAccess.kt` |
| `GroupMetadataMessageSender` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMetadataMessageSender.kt` |
| `GroupMetadataSendContext` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMetadataSecurityContext.kt` |
| `OpenedGroupWelcomeDto` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupSecurityModels.kt` |
| `SecuredGroupMessageDto` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupSecurityModels.kt` |
| `GroupTransportRoutingMember` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupTransportRoutingMember.kt` |
| `GroupVerificationMemberKey` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupVerificationMembership` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupVerificationMembershipContext` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupVerificationSecurityState` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupWelcomeMemberKey` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupWelcomeMemberKey.kt` |
| `IncomingMembershipOffer` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipDeclineDisposition` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipDeclineResult` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipHandshake` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipJoinRequest` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipSigningProof` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `StartedMembershipHandshake` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipResult` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipResult.kt` |
| `MembershipPerspective` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipStatus.kt` |
| `MembershipStatus` | enum class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipStatus.kt` |
| `MembershipVerificationSnapshot` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipVerificationSnapshot.kt` |
| `OpenedIncomingGroupWelcome` | data class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/OpenedIncomingGroupWelcome.kt` |
| `GroupMembershipRepository` | interface | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/repository/GroupMembershipRepository.kt` |
| `GroupSecurityRepository` | interface | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/repository/GroupSecurityRepository.kt` |
| `MembershipRepository` | interface | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/repository/MembershipRepository.kt` |
| `AcceptGroupMembershipUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AcceptGroupMembershipUseCase.kt` |
| `ApplyIncomingGroupActivationUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ApplyIncomingGroupActivationUseCase.kt` |
| `AuthorizeGroupMetadataUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeGroupMetadataUseCase.kt` |
| `AuthorizeIncomingGroupActivationUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupActivationUseCase.kt` |
| `AuthorizeIncomingGroupDeletionUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupDeletionUseCase.kt` |
| `AuthorizeIncomingGroupRemovalUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupRemovalUseCase.kt` |
| `AuthorizeIncomingGroupWelcomeUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupWelcomeUseCase.kt` |
| `ClearMembershipHandshakeUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ClearMembershipHandshakeUseCase.kt` |
| `CompleteIncomingGroupDeletionUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/CompleteIncomingGroupDeletionUseCase.kt` |
| `CompleteIncomingGroupRemovalUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/CompleteIncomingGroupRemovalUseCase.kt` |
| `CompleteIncomingGroupWelcomeUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/CompleteIncomingGroupWelcomeUseCase.kt` |
| `ConfirmGroupMembershipIdentityUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ConfirmGroupMembershipIdentityUseCase.kt` |
| `DeclineGroupMembershipUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/DeclineGroupMembershipUseCase.kt` |
| `DeleteGroupMembershipUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/DeleteGroupMembershipUseCase.kt` |
| `DiscardSupersededMembershipsUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/DiscardSupersededMembershipsUseCase.kt` |
| `GetGroupCurrentEpochUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupCurrentEpochUseCase.kt` |
| `GetGroupLeaveRequirementUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupLeaveRequirementUseCase.kt` |
| `GetGroupMessageMembershipAccessUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupMessageMembershipAccessUseCase.kt` |
| `GetGroupPinSenderSigningKeyUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupPinSenderSigningKeyUseCase.kt` |
| `GetGroupTransportRoutingMembersUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupTransportRoutingMembersUseCase.kt` |
| `GetMembershipHandshakeUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetMembershipHandshakeUseCase.kt` |
| `InspectIncomingGroupMembershipUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/InspectIncomingGroupMembershipUseCase.kt` |
| `LeaveGroupUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/LeaveGroupUseCase.kt` |
| `MarkMembershipRemovedUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/MarkMembershipRemovedUseCase.kt` |
| `ObserveGroupAdministrationUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ObserveGroupAdministrationUseCase.kt` |
| `ObserveMembershipResultsUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ObserveMembershipResultsUseCase.kt` |
| `OpenIncomingGroupWelcomeUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/OpenIncomingGroupWelcomeUseCase.kt` |
| `PersistIncomingGroupWelcomeUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/PersistIncomingGroupWelcomeUseCase.kt` |
| `PromoteGroupMemberUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/PromoteGroupMemberUseCase.kt` |
| `ReceiveGroupActivationAcknowledgementUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupActivationAcknowledgementUseCase.kt` |
| `ReceiveGroupLeaveRequestUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupLeaveRequestUseCase.kt` |
| `ReceiveGroupMembershipDeclineUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupMembershipDeclineUseCase.kt` |
| `ReceiveGroupMembershipJoinRequestUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupMembershipJoinRequestUseCase.kt` |
| `ReceiveGroupMembershipReceiptUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupMembershipReceiptUseCase.kt` |
| `ReceiveGroupReadyAcknowledgementUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupReadyAcknowledgementUseCase.kt` |
| `ReceiveIncomingGroupMembershipUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveIncomingGroupMembershipUseCase.kt` |
| `RemoveGroupMemberUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/RemoveGroupMemberUseCase.kt` |
| `ResolveGroupTransportEncryptionPublicKeyUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ResolveGroupTransportEncryptionPublicKeyUseCase.kt` |
| `SendGroupReadyAcknowledgementUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/SendGroupReadyAcknowledgementUseCase.kt` |
| `StartGroupMembershipUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/StartGroupMembershipUseCase.kt` |
| `TransferGroupAdminAndLeaveUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/TransferGroupAdminAndLeaveUseCase.kt` |
| `VerifyGroupKeyConfirmationUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/VerifyGroupKeyConfirmationUseCase.kt` |
| `WasGroupMembershipDeletedUseCase` | class | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/WasGroupMembershipDeletedUseCase.kt` |
| `GroupActivationPolicyTest` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/GroupActivationPolicyTest.kt` |
| `GroupMembershipStateMachineTest` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipStateMachineTest.kt` |
| `GroupPacketBroadcasterTest` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupPacketBroadcasterTest.kt` |
| `RecordingProtocolOutbox` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupPacketBroadcasterTest.kt` |
| `PendingGroupRemovalPolicyTest` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/policy/PendingGroupRemovalPolicyTest.kt` |
| `DeterministicGroupCrypto` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/protocol/GroupMembershipPacketProtocolTest.kt` |
| `GroupMembershipPacketProtocolTest` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/protocol/GroupMembershipPacketProtocolTest.kt` |
| `DeterministicGroupCrypto` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupSecurityManagerTest.kt` |
| `GroupSecurityManagerTest` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupSecurityManagerTest.kt` |
| `InMemoryGroupKeyStore` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupSecurityManagerTest.kt` |
| `InMemoryGroupSecurityDao` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupSecurityManagerTest.kt` |
| `GroupConversationMembershipProjectorTest` | class | `commonTest` | `feature/membership/src/commonTest/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipProjectorTest.kt` |

### `:feature:attachments`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `MessageAttachmentFileDataSourceTest` | class | `androidDeviceTest` | `feature/attachments/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/MessageAttachmentFileDataSourceTest.kt` |
| `AndroidLocationOpener` | class | `androidMain` | `feature/attachments/src/androidMain/kotlin/com/cbgm/sparrow/feature/attachments/device/LocationOpener.android.kt` |
| `AttachmentContentDataSource` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/AttachmentContentDataSource.kt` |
| `BlobTransferDataSource` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/BlobTransferDataSource.kt` |
| `LocalAttachmentDataSource` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/LocalAttachmentDataSource.kt` |
| `StoredPart` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/LocalAttachmentDataSource.kt` |
| `MessageAttachmentDataSource` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/MessageAttachmentDataSource.kt` |
| `MessageAttachmentFileDataSource` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/MessageAttachmentFileDataSource.kt` |
| `MessagePartFileMetadata` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/mapper/MessagePartFileMetadataMapper.kt` |
| `AttachmentMessageContextDto` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentMessageContextDto.kt` |
| `AttachmentStorageSummaryDto` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentStorageSummaryDto.kt` |
| `MessageAttachmentOperationsRepositoryImpl` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/repository/MessageAttachmentOperationsRepositoryImpl.kt` |
| `MessageAttachmentRepositoryImpl` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/repository/MessageAttachmentRepositoryImpl.kt` |
| `CurrentLocationLauncher` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/device/CurrentLocationLauncher.kt` |
| `LocationOpener` | interface | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/device/LocationOpener.kt` |
| `AttachmentMessageContext` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentMessageContext.kt` |
| `AttachmentStorageSummary` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentStorageSummary.kt` |
| `AttachmentTranscript` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTranscript.kt` |
| `AttachmentTranscriptCue` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTranscript.kt` |
| `CurrentLocation` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/CurrentLocation.kt` |
| `SharedContact` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/SharedContact.kt` |
| `MessageAttachmentOperationsRepository` | interface | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/repository/MessageAttachmentOperationsRepository.kt` |
| `MessageAttachmentRepository` | interface | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/repository/MessageAttachmentRepository.kt` |
| `DeleteLocalAttachmentsUseCase` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/DeleteLocalAttachmentsUseCase.kt` |
| `LoadAttachmentBytesUseCase` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/LoadAttachmentBytesUseCase.kt` |
| `LoadAttachmentContentUseCase` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/LoadAttachmentContentUseCase.kt` |
| `ObserveAttachmentStorageSummariesUseCase` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/ObserveAttachmentStorageSummariesUseCase.kt` |
| `ObserveLocalAttachmentsUseCase` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/ObserveLocalAttachmentsUseCase.kt` |
| `ObserveMessageAttachmentTranscriptUseCase` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/ObserveMessageAttachmentTranscriptUseCase.kt` |
| `SaveMessageAttachmentTranscriptUseCase` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/SaveMessageAttachmentTranscriptUseCase.kt` |
| `AttachmentViewModel` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/AttachmentViewModel.kt` |
| `AttachmentManagementLocalState` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/AttachmentManagementViewModel.kt` |
| `AttachmentManagementViewModel` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/AttachmentManagementViewModel.kt` |
| `AttachmentSnapshot` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/AttachmentManagementViewModel.kt` |
| `AttachmentManagementTab` | enum class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementTab.kt` |
| `AttachmentClicked` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `AttachmentManagementUiEvent` | interface | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `DeleteConfirmed` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `DeleteDismissed` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `DeleteSelectedClicked` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `SelectionCleared` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `SelectionStarted` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `TabSelected` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `ViewerDismissed` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `ViewerError` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `AttachmentManagementUiState` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiState.kt` |
| `AttachmentUiState` | interface | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/model/AttachmentUiState.kt` |
| `Error` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/model/AttachmentUiState.kt` |
| `Idle` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/model/AttachmentUiState.kt` |
| `Loading` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/model/AttachmentUiState.kt` |
| `Ready` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/model/AttachmentUiState.kt` |
| `AttachmentStorageViewModel` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/AttachmentStorageViewModel.kt` |
| `AttachmentStorageUiEvent` | interface | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiEvent.kt` |
| `ConversationClicked` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiEvent.kt` |
| `AttachmentStorageUiState` | interface | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiState.kt` |
| `Content` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiState.kt` |
| `Error` | data class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiState.kt` |
| `Loading` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiState.kt` |
| `MessageAttachmentCacheCoordinator` | class | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/runtime/MessageAttachmentCacheCoordinator.kt` |
| `ContactAttachmentPayload` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/util/ContactAttachmentPayload.kt` |
| `LocationAttachmentPayload` | object | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/util/LocationAttachmentPayload.kt` |
| `MessagePartPersistenceMapperTest` | class | `commonTest` | `feature/attachments/src/commonTest/kotlin/com/cbgm/sparrow/feature/attachments/data/mapper/MessagePartPersistenceMapperTest.kt` |
| `MessagePartMediaMapperTest` | class | `commonTest` | `feature/attachments/src/commonTest/kotlin/com/cbgm/sparrow/feature/attachments/presentation/mapper/MessagePartMediaMapperTest.kt` |
| `CurrentLocationDelegate` | class | `iosMain` | `feature/attachments/src/iosMain/kotlin/com/cbgm/sparrow/feature/attachments/device/CurrentLocationLauncher.ios.kt` |
| `IosLocationOpener` | class | `iosMain` | `feature/attachments/src/iosMain/kotlin/com/cbgm/sparrow/feature/attachments/device/LocationOpener.ios.kt` |

### `:feature:media`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AndroidFileBrowserDataSource` | class | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/AndroidFileBrowserDataSource.kt` |
| `AndroidMediaSelectionFileDataSource` | class | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/AndroidMediaSelectionFileDataSource.kt` |
| `UprightLuminance` | class | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/CameraFrameLuminance.android.kt` |
| `AndroidFileOpener` | class | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.android.kt` |
| `AndroidMediaExporter` | class | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/MediaExport.android.kt` |
| `FileBrowserDataSource` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/datasource/FileBrowserDataSource.kt` |
| `MediaSelectionFileDataSource` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/datasource/MediaSelectionFileDataSource.kt` |
| `FileBrowserContentDto` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/FileBrowserContentDto.kt` |
| `FileBrowserDirectoryDto` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/FileBrowserDirectoryDto.kt` |
| `FileBrowserEntryDto` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/FileBrowserEntryDto.kt` |
| `StoredMediaSelectionPathsDto` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/StoredMediaSelectionPathsDto.kt` |
| `FileBrowserRepositoryImpl` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/repository/FileBrowserRepositoryImpl.kt` |
| `MediaSelectionFileRepositoryImpl` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/repository/MediaSelectionFileRepositoryImpl.kt` |
| `CameraCaptureLauncher` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/CameraCapture.kt` |
| `CameraScannerLauncher` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/CameraScanner.kt` |
| `DocumentFrameDetector` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/DocumentFrameDetector.kt` |
| `DocumentFrameState` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/DocumentFrameDetector.kt` |
| `Frame` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/DocumentFrameDetector.kt` |
| `FileAccessLauncher` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/FileAccessLauncher.kt` |
| `FileOpener` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.kt` |
| `GalleryPickerLauncher` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.kt` |
| `GalleryPickerStrings` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.kt` |
| `MediaExporter` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/MediaExport.kt` |
| `QrCodeImageLayout` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/QrCodeImageLayout.kt` |
| `QrCodeRenderRequest` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/QrCodeRenderRequest.kt` |
| `QrErrorCorrection` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/QrCodeRenderRequest.kt` |
| `CameraCaptureConfig` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CameraCaptureType` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CameraLens` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CapturedMedia` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CameraScanMode` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraScan.kt` |
| `CameraScanResult` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraScan.kt` |
| `Document` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraScan.kt` |
| `Document` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraScan.kt` |
| `DocumentCaptureMode` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraScan.kt` |
| `QrCode` | object | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraScan.kt` |
| `QrCode` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraScan.kt` |
| `FileBrowserContent` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/FileBrowserContent.kt` |
| `FileBrowserDirectory` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/FileBrowserDirectory.kt` |
| `FileBrowserEntry` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/FileBrowserEntry.kt` |
| `GalleryMedia` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/GalleryMedia.kt` |
| `GalleryPickerConfig` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/GalleryMedia.kt` |
| `MediaContentType` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaContentType.kt` |
| `MediaExportItem` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaExportItem.kt` |
| `FileMediaSelection` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaSelection.kt` |
| `MediaSelection` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaSelection.kt` |
| `MediaSource` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaSelection.kt` |
| `VisualMediaSelection` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaSelection.kt` |
| `StoredMediaSelectionPaths` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/StoredMediaSelectionPaths.kt` |
| `FileBrowserRepository` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/repository/FileBrowserRepository.kt` |
| `MediaSelectionFileRepository` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/repository/MediaSelectionFileRepository.kt` |
| `BrowseFileDirectoryUseCase` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/BrowseFileDirectoryUseCase.kt` |
| `CheckFileBrowserAccessUseCase` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/CheckFileBrowserAccessUseCase.kt` |
| `GetFileBrowserRootUseCase` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/GetFileBrowserRootUseCase.kt` |
| `PrepareMediaSelectionUseCase` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/PrepareMediaSelectionUseCase.kt` |
| `ReadFileBrowserEntryUseCase` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/ReadFileBrowserEntryUseCase.kt` |
| `SetFileBrowserRootUseCase` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/SetFileBrowserRootUseCase.kt` |
| `DocumentCaptureControls` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/camera/CameraScannerControls.kt` |
| `ScanGuideShape` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/camera/CameraScannerControls.kt` |
| `FilePickerLauncher` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerLauncher.kt` |
| `FilePickerSession` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerSessionController.kt` |
| `FilePickerSessionController` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerSessionController.kt` |
| `FilePickerSessionSnapshotUi` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerSessionController.kt` |
| `DirectoryState` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerViewModel.kt` |
| `FilePickerViewModel` | class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerViewModel.kt` |
| `Completed` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerSessionResultUi.kt` |
| `Dismissed` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerSessionResultUi.kt` |
| `Failed` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerSessionResultUi.kt` |
| `FilePickerSessionResultUi` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerSessionResultUi.kt` |
| `BackClicked` | object | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `BreadcrumbClicked` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `CloseClicked` | object | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `ConfirmClicked` | object | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `EntryClicked` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `FileAccessError` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `FileAccessReturned` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `FilePickerUiEvent` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `GrantFileAccessClicked` | object | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `SearchChanged` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `SearchCleared` | object | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `SortDirectionToggled` | object | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `SortSelected` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `FileBrowserEntryKindUi` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FileBrowserEntryUi` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FilePickerBreadcrumbUi` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FilePickerSortModeUi` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FilePickerUiState` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `MediaItemUi` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaItemUi.kt` |
| `MediaTypeUi` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaItemUi.kt` |
| `VisualMediaUi` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaItemUi.kt` |
| `Dismissed` | object | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionResultUi.kt` |
| `Error` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionResultUi.kt` |
| `MediaSelectionResultUi` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionResultUi.kt` |
| `Selected` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionResultUi.kt` |
| `FileMediaSelectionUi` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionUi.kt` |
| `MediaSelectionUi` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionUi.kt` |
| `MediaSourceUi` | enum class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionUi.kt` |
| `VisualMediaSelectionUi` | data class | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionUi.kt` |
| `DocumentScanLauncher` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/selection/DocumentScanLauncher.kt` |
| `MediaSelectionLauncher` | interface | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/selection/MediaSelectionLauncher.kt` |
| `DocumentFrameDetectorTest` | class | `commonTest` | `feature/media/src/commonTest/kotlin/com/cbgm/sparrow/feature/media/device/DocumentFrameDetectorTest.kt` |
| `QrCodeImageLayoutTest` | class | `commonTest` | `feature/media/src/commonTest/kotlin/com/cbgm/sparrow/feature/media/device/QrCodeImageLayoutTest.kt` |
| `FilePickerEntriesUiTest` | class | `commonTest` | `feature/media/src/commonTest/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerEntriesUiTest.kt` |
| `MediaUiMapperTest` | class | `commonTest` | `feature/media/src/commonTest/kotlin/com/cbgm/sparrow/feature/media/presentation/mapper/MediaUiMapperTest.kt` |
| `MediaModelsTest` | class | `commonTest` | `feature/media/src/commonTest/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaModelsTest.kt` |
| `CameraPickerDelegate` | class | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/CameraCapture.ios.kt` |
| `FileAccessDelegate` | class | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/FileAccessLauncher.ios.kt` |
| `DocumentInteractionDelegate` | class | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.ios.kt` |
| `IosFileOpener` | class | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.ios.kt` |
| `GalleryPickerDelegate` | class | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.ios.kt` |
| `GallerySelectionCollector` | class | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.ios.kt` |
| `IosFileBrowserDataSource` | class | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/IosFileBrowserDataSource.kt` |
| `IosMediaSelectionFileDataSource` | class | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/IosMediaSelectionFileDataSource.kt` |
| `VideoView` | class | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/PlatformVideoMedia.ios.kt` |

### `:feature:ocr`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AndroidTextRecognizer` | class | `androidMain` | `feature/ocr/src/androidMain/kotlin/com/cbgm/sparrow/feature/ocr/device/AndroidTextRecognizer.kt` |
| `RecognizedTextLine` | data class | `commonMain` | `feature/ocr/src/commonMain/kotlin/com/cbgm/sparrow/feature/ocr/data/model/RecognizedTextLine.kt` |
| `AmountCell` | class | `commonMain` | `feature/ocr/src/commonMain/kotlin/com/cbgm/sparrow/feature/ocr/data/parser/ReceiptTextParser.kt` |
| `ReceiptTextParser` | object | `commonMain` | `feature/ocr/src/commonMain/kotlin/com/cbgm/sparrow/feature/ocr/data/parser/ReceiptTextParser.kt` |
| `ReceiptScanRepositoryImpl` | class | `commonMain` | `feature/ocr/src/commonMain/kotlin/com/cbgm/sparrow/feature/ocr/data/repository/ReceiptScanRepositoryImpl.kt` |
| `TextRecognizer` | interface | `commonMain` | `feature/ocr/src/commonMain/kotlin/com/cbgm/sparrow/feature/ocr/device/TextRecognizer.kt` |
| `ReceiptScan` | data class | `commonMain` | `feature/ocr/src/commonMain/kotlin/com/cbgm/sparrow/feature/ocr/domain/model/ReceiptScan.kt` |
| `ReceiptScanRepository` | interface | `commonMain` | `feature/ocr/src/commonMain/kotlin/com/cbgm/sparrow/feature/ocr/domain/repository/ReceiptScanRepository.kt` |
| `ScanReceiptUseCase` | class | `commonMain` | `feature/ocr/src/commonMain/kotlin/com/cbgm/sparrow/feature/ocr/domain/usecase/ScanReceiptUseCase.kt` |
| `ReceiptTextParserTest` | class | `commonTest` | `feature/ocr/src/commonTest/kotlin/com/cbgm/sparrow/feature/ocr/data/parser/ReceiptTextParserTest.kt` |
| `IosTextRecognizer` | class | `iosMain` | `feature/ocr/src/iosMain/kotlin/com/cbgm/sparrow/feature/ocr/device/IosTextRecognizer.kt` |

### `:feature:voice`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AndroidVoicePlayer` | class | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoicePlayer.kt` |
| `ByteArrayMediaDataSource` | class | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoicePlayer.kt` |
| `AndroidVoiceRecorder` | class | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoiceRecorder.kt` |
| `AndroidVoiceTranscriptionRepository` | class | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoiceTranscriptionRepository.kt` |
| `AndroidWhisperModelStore` | class | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidWhisperModelStore.kt` |
| `WhisperNative` | class | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/WhisperNative.kt` |
| `WhisperProgressCallback` | interface | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/WhisperNative.kt` |
| `VoiceTranscriptionSettingsDataSource` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/data/datasource/VoiceTranscriptionSettingsDataSource.kt` |
| `VoiceRepositoryImpl` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/data/repository/VoiceRepositoryImpl.kt` |
| `VoiceTranscriptionSettingsRepositoryImpl` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/data/repository/VoiceTranscriptionSettingsRepositoryImpl.kt` |
| `PcmWaveAudio` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/device/PcmWaveAudio.kt` |
| `VoicePlayer` | interface | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/device/VoicePlayer.kt` |
| `VoiceRecorder` | interface | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/device/VoiceRecorder.kt` |
| `VoiceRecording` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceRecording.kt` |
| `Downloading` | object | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `Error` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `Idle` | object | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `Preparing` | object | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `Success` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `Transcribing` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceComposerPhase` | enum class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceComposerState` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoicePlaybackState` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceTranscriptionState` | interface | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceTranscript` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceTranscript.kt` |
| `VoiceTranscriptCue` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceTranscript.kt` |
| `VoiceRepository` | interface | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/repository/VoiceRepository.kt` |
| `VoiceTranscriptionRepository` | interface | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/repository/VoiceTranscriptionRepository.kt` |
| `VoiceTranscriptionSettingsRepository` | interface | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/repository/VoiceTranscriptionSettingsRepository.kt` |
| `CancelVoiceRecordingUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/CancelVoiceRecordingUseCase.kt` |
| `FinishVoiceMessageScrubUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/FinishVoiceMessageScrubUseCase.kt` |
| `GetRecordedVoiceAttachmentUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/GetRecordedVoiceAttachmentUseCase.kt` |
| `ObserveVoiceComposerUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoiceComposerUseCase.kt` |
| `ObserveVoicePlaybackUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoicePlaybackUseCase.kt` |
| `ObserveVoiceRecordingActiveUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoiceRecordingActiveUseCase.kt` |
| `ObserveVoiceTranscriptionEnabledUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoiceTranscriptionEnabledUseCase.kt` |
| `PrepareVoiceTranscriptionUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/PrepareVoiceTranscriptionUseCase.kt` |
| `ResetVoiceComposerUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ResetVoiceComposerUseCase.kt` |
| `SetVoiceTranscriptionEnabledUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/SetVoiceTranscriptionEnabledUseCase.kt` |
| `StartVoiceMessageScrubUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/StartVoiceMessageScrubUseCase.kt` |
| `StartVoiceRecordingUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/StartVoiceRecordingUseCase.kt` |
| `StopVoiceRecordingUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/StopVoiceRecordingUseCase.kt` |
| `ToggleVoiceMessagePlaybackUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ToggleVoiceMessagePlaybackUseCase.kt` |
| `ToggleVoicePreviewUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ToggleVoicePreviewUseCase.kt` |
| `TranscribeVoiceAudioUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceAudioUseCase.kt` |
| `TranscribeVoiceMessageUseCase` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceMessageUseCase.kt` |
| `VoiceTranscriptionPhase` | enum class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceMessageUseCase.kt` |
| `VoiceTranscriptionPhaseException` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceMessageUseCase.kt` |
| `VoiceAction` | enum class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/composer/VoiceComposer.kt` |
| `VoiceComposerViewModel` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/composer/VoiceComposerViewModel.kt` |
| `VoiceComposerUiState` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/composer/model/VoiceComposerUiState.kt` |
| `VoiceScrubState` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/message/VoiceMessageContent.kt` |
| `VoiceMessageViewModel` | class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/message/VoiceMessageViewModel.kt` |
| `VoiceMessageUiState` | data class | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/message/model/VoiceMessageUiState.kt` |
| `PcmWaveAudioTest` | class | `commonTest` | `feature/voice/src/commonTest/kotlin/com/cbgm/sparrow/feature/voice/device/PcmWaveAudioTest.kt` |
| `IosVoicePlayer` | class | `iosMain` | `feature/voice/src/iosMain/kotlin/com/cbgm/sparrow/feature/voice/device/IosVoicePlayer.kt` |
| `IosVoiceRecorder` | class | `iosMain` | `feature/voice/src/iosMain/kotlin/com/cbgm/sparrow/feature/voice/device/IosVoiceRecorder.kt` |
| `IosVoiceTranscriptionRepository` | class | `iosMain` | `feature/voice/src/iosMain/kotlin/com/cbgm/sparrow/feature/voice/device/IosVoiceTranscriptionRepository.kt` |

### `:feature:polls`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `PollComposerRepositoryImpl` | class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/data/repository/PollComposerRepositoryImpl.kt` |
| `PollComposerRepository` | interface | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/domain/repository/PollComposerRepository.kt` |
| `ClearFinishedPollUseCase` | class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/domain/usecase/ClearFinishedPollUseCase.kt` |
| `FinishPollUseCase` | class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/domain/usecase/FinishPollUseCase.kt` |
| `ObserveFinishedPollUseCase` | class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/domain/usecase/ObserveFinishedPollUseCase.kt` |
| `CreatePollViewModel` | class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/CreatePollViewModel.kt` |
| `AddOptionClicked` | object | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `AnonymousChanged` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `CreateClicked` | object | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `CreatePollUiEvent` | interface | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `DescriptionChanged` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `ExpiryEnabledChanged` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `ExpiryMinutesChanged` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `MediaSelectionChanged` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `MultipleSelectionChanged` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `OptionChanged` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `QuestionChanged` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `RemoveMediaClicked` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `RemoveOptionClicked` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `VoteChangeChanged` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiEvent.kt` |
| `CreatePollUiState` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiState.kt` |
| `PollOptionEditorUi` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/create/model/CreatePollUiState.kt` |
| `PollMessageViewModel` | class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/message/PollMessageViewModel.kt` |
| `PollMessageUiState` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/message/model/PollMessageUiState.kt` |
| `PollOptionUi` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/message/model/PollMessageUiState.kt` |
| `PollVoterUi` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/model/PollVoterUi.kt` |
| `PollVoterSectionUi` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/voters/model/PollVotersUiState.kt` |
| `PollVotersUiState` | data class | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/presentation/voters/model/PollVotersUiState.kt` |
| `PollConstants` | object | `commonMain` | `feature/polls/src/commonMain/kotlin/com/cbgm/sparrow/feature/polls/util/PollConstants.kt` |

### `:feature:expenses`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `LocalExchangeRateDataSource` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/data/datasource/LocalExchangeRateDataSource.kt` |
| `RemoteExchangeRateDataSource` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/data/datasource/RemoteExchangeRateDataSource.kt` |
| `ExchangeRateDto` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/data/model/ExchangeRateDto.kt` |
| `ExchangeRatesCacheDto` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/data/model/ExchangeRateDto.kt` |
| `ExchangeRateRepositoryImpl` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/data/repository/ExchangeRateRepositoryImpl.kt` |
| `ExpenseComposerRepositoryImpl` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/data/repository/ExpenseComposerRepositoryImpl.kt` |
| `ExpenseCurrencies` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/ExpenseCurrencies.kt` |
| `ExpenseMoney` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/ExpenseMoney.kt` |
| `ExpensePayments` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/ExpensePayments.kt` |
| `ExpenseRevisions` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/ExpenseRevisions.kt` |
| `ExchangeRates` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExchangeRates.kt` |
| `ExpenseBalance` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseBalance.kt` |
| `ExpenseSettlementSuggestion` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseBalance.kt` |
| `ExpenseCreationContext` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseCreationContext.kt` |
| `ExpenseMember` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseCreationContext.kt` |
| `ExpenseGroup` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseGroup.kt` |
| `ExpenseOverview` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseOverview.kt` |
| `Create` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseRequest.kt` |
| `Delete` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseRequest.kt` |
| `Edit` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseRequest.kt` |
| `ExpenseRequest` | interface | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseRequest.kt` |
| `ExpenseSummary` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/model/ExpenseSummary.kt` |
| `ExchangeRateRepository` | interface | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/repository/ExchangeRateRepository.kt` |
| `ExpenseComposerRepository` | interface | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/repository/ExpenseComposerRepository.kt` |
| `CalculateExpenseBalancesUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/CalculateExpenseBalancesUseCase.kt` |
| `CalculateExpenseSettlementsUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/CalculateExpenseSettlementsUseCase.kt` |
| `RemainingAmount` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/CalculateExpenseSettlementsUseCase.kt` |
| `ClearExpenseGroupUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/ClearExpenseGroupUseCase.kt` |
| `GetExpenseCreationContextUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/GetExpenseCreationContextUseCase.kt` |
| `ObserveExchangeRatesUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/ObserveExchangeRatesUseCase.kt` |
| `ObserveExpenseOverviewUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/ObserveExpenseOverviewUseCase.kt` |
| `ObserveExpenseRequestsUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/ObserveExpenseRequestsUseCase.kt` |
| `ObserveExpenseSummaryUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/ObserveExpenseSummaryUseCase.kt` |
| `PublishExpenseGroupUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/PublishExpenseGroupUseCase.kt` |
| `RefreshExchangeRatesUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/RefreshExchangeRatesUseCase.kt` |
| `SubmitExpenseRequestUseCase` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/SubmitExpenseRequestUseCase.kt` |
| `CreateExpenseViewModel` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/CreateExpenseViewModel.kt` |
| `AddAnotherClicked` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `AmountChanged` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `BackClicked` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `CategorySelected` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `CreateExpenseUiEvent` | interface | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `CurrencySelected` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `CustomAmountChanged` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `DescriptionChanged` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `ParticipantToggled` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `PayerSelected` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `ReceiptScanned` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `RemoveQueuedClicked` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `RemoveReceiptClicked` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `SendClicked` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `SplitDoneClicked` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `SplitModeSelected` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `SplitPickerClicked` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiEvent.kt` |
| `CreateExpenseUiState` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiState.kt` |
| `ExpenseCategoryUi` | enum class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiState.kt` |
| `ExpenseCurrencyUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiState.kt` |
| `ExpenseDraftUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiState.kt` |
| `ExpenseParticipantUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiState.kt` |
| `ExpenseSplitModeUi` | enum class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/model/CreateExpenseUiState.kt` |
| `ExpenseCurrencyPickerViewModel` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/currency/ExpenseCurrencyPickerViewModel.kt` |
| `ExpenseDetailViewModel` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/detail/ExpenseDetailViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/detail/model/ExpenseDetailUiEvent.kt` |
| `DeleteConfirmed` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/detail/model/ExpenseDetailUiEvent.kt` |
| `EditClicked` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/detail/model/ExpenseDetailUiEvent.kt` |
| `ExpenseDetailUiEvent` | interface | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/detail/model/ExpenseDetailUiEvent.kt` |
| `ExpenseDetailUiState` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/detail/model/ExpenseDetailUiState.kt` |
| `ExpenseShareUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/detail/model/ExpenseDetailUiState.kt` |
| `ExpenseOverviewViewModel` | class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/ExpenseOverviewViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiEvent.kt` |
| `DeleteExpenseConfirmed` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiEvent.kt` |
| `EditExpenseClicked` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiEvent.kt` |
| `ExpenseClicked` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiEvent.kt` |
| `ExpenseOverviewUiEvent` | interface | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiEvent.kt` |
| `PaymentClicked` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiEvent.kt` |
| `Borrowed` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `ExpenseAmountUi` | interface | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `ExpenseListItemUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `ExpenseMemberBalanceUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `ExpenseOverviewUiState` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `ExpensePaymentUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `ExpenseSettlementUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `ExpenseYourBalanceUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `ExpenseYourPartUi` | interface | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `Lent` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `NotInvolved` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `Owed` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `Owes` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `Settled` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/model/ExpenseOverviewUiState.kt` |
| `ExpensePinBalanceUi` | interface | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/pin/model/ExpensePinUi.kt` |
| `ExpensePinUi` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/pin/model/ExpensePinUi.kt` |
| `Owed` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/pin/model/ExpensePinUi.kt` |
| `Owes` | data class | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/pin/model/ExpensePinUi.kt` |
| `Settled` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/pin/model/ExpensePinUi.kt` |
| `Unavailable` | object | `commonMain` | `feature/expenses/src/commonMain/kotlin/com/cbgm/sparrow/feature/expenses/presentation/pin/model/ExpensePinUi.kt` |
| `ExchangeRateMapperTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/data/mapper/ExchangeRateMapperTest.kt` |
| `ExpenseMoneyTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/domain/ExpenseMoneyTest.kt` |
| `ExpenseRevisionsTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/domain/ExpenseRevisionsTest.kt` |
| `ExpenseCalculationTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/ExpenseCalculationTest.kt` |
| `ExpensePaymentUpdatesTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/ExpensePaymentUpdatesTest.kt` |
| `ObserveExpenseSummaryUseCaseTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/ObserveExpenseSummaryUseCaseTest.kt` |
| `PaymentSettlementBalanceTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/domain/usecase/PaymentSettlementBalanceTest.kt` |
| `ExpenseAmountMapperTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/presentation/common/mapper/ExpenseAmountMapperTest.kt` |
| `CreateExpenseUiStateTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/CreateExpenseUiStateTest.kt` |
| `ExpenseParticipantAvatarMapperTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/kmapper/ExpenseParticipantAvatarMapperTest.kt` |
| `ExpenseCreationMapperTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/presentation/create/mapper/ExpenseCreationMapperTest.kt` |
| `ExpenseOverviewUiMapperTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/presentation/overview/mapper/ExpenseOverviewUiMapperTest.kt` |
| `ExpensePinUiMapperTest` | class | `commonTest` | `feature/expenses/src/commonTest/kotlin/com/cbgm/sparrow/feature/expenses/presentation/pin/mapper/ExpensePinUiMapperTest.kt` |

### `:feature:payment`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AndroidBankDetailsStorageTest` | class | `androidDeviceTest` | `feature/payment/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/payment/device/AndroidBankDetailsStorageTest.kt` |
| `PaymentQrImageTest` | class | `androidDeviceTest` | `feature/payment/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/payment/device/PaymentQrImageTest.kt` |
| `AndroidBankDetailsStorage` | class | `androidMain` | `feature/payment/src/androidMain/kotlin/com/cbgm/sparrow/feature/payment/device/AndroidBankDetailsStorage.kt` |
| `PaymentFileProvider` | class | `androidMain` | `feature/payment/src/androidMain/kotlin/com/cbgm/sparrow/feature/payment/device/PaymentFileProvider.kt` |
| `BankDetailsDto` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/data/model/BankDetailsDto.kt` |
| `BankDetailsRepositoryImpl` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/data/repository/BankDetailsRepositoryImpl.kt` |
| `Completed` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/data/repository/PaymentComposerRepositoryImpl.kt` |
| `ComposerState` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/data/repository/PaymentComposerRepositoryImpl.kt` |
| `PaymentComposerRepositoryImpl` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/data/repository/PaymentComposerRepositoryImpl.kt` |
| `Pending` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/data/repository/PaymentComposerRepositoryImpl.kt` |
| `RequestState` | interface | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/data/repository/PaymentComposerRepositoryImpl.kt` |
| `BankDetailsStorage` | interface | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/device/BankDetailsStorage.kt` |
| `PaymentImageLabels` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/device/PaymentImageLabels.kt` |
| `PaymentImageRequest` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/device/PaymentImageLauncher.kt` |
| `SepaPaymentPolicy` | object | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/SepaPaymentPolicy.kt` |
| `BankDetails` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/BankDetails.kt` |
| `PaymentContext` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/PaymentContext.kt` |
| `PaymentMember` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/PaymentContext.kt` |
| `PaymentSettlement` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/PaymentContext.kt` |
| `InvalidPaymentDetailsException` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/PaymentFailure.kt` |
| `PaymentUnavailableException` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/PaymentFailure.kt` |
| `PaymentRequest` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/PaymentRequest.kt` |
| `PaymentSelection` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/PaymentSelection.kt` |
| `PaymentSession` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/PaymentSession.kt` |
| `PreparedPayment` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/PreparedPayment.kt` |
| `SepaPayment` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/model/SepaPayment.kt` |
| `BankDetailsRepository` | interface | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/repository/BankDetailsRepository.kt` |
| `PaymentComposerRepository` | interface | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/repository/PaymentComposerRepository.kt` |
| `ClearPaymentContextUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/ClearPaymentContextUseCase.kt` |
| `CompletePaymentRequestUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/CompletePaymentRequestUseCase.kt` |
| `DeleteBankDetailsUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/DeleteBankDetailsUseCase.kt` |
| `FindBankDetailsUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/FindBankDetailsUseCase.kt` |
| `ObservePaymentContextUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/ObservePaymentContextUseCase.kt` |
| `ObservePaymentRequestsUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/ObservePaymentRequestsUseCase.kt` |
| `ObservePaymentSessionUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/ObservePaymentSessionUseCase.kt` |
| `PreparePaymentUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/PreparePaymentUseCase.kt` |
| `PublishPaymentContextUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/PublishPaymentContextUseCase.kt` |
| `RecordPaymentStatusUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/RecordPaymentStatusUseCase.kt` |
| `RecordSelectedPaymentUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/RecordSelectedPaymentUseCase.kt` |
| `SaveBankDetailsUseCase` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/SaveBankDetailsUseCase.kt` |
| `PaymentViewModel` | class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/PaymentViewModel.kt` |
| `PaymentBankDetailsUi` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentBankDetailsUi.kt` |
| `PaymentReviewUi` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentReviewUi.kt` |
| `PaymentSelectionUi` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentSelectionUi.kt` |
| `PaymentSessionUi` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentSessionUi.kt` |
| `Back` | object | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `ConfirmReceipt` | object | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `Edit` | object | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `ForgetDetails` | object | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `HolderChanged` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `IbanChanged` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `MarkSent` | object | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `PaymentUiEvent` | interface | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `RememberChanged` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `Review` | object | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `Share` | object | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `ShareCompleted` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiEvent.kt` |
| `PaymentErrorUi` | enum class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiState.kt` |
| `PaymentStatusUi` | enum class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiState.kt` |
| `PaymentUiState` | data class | `commonMain` | `feature/payment/src/commonMain/kotlin/com/cbgm/sparrow/feature/payment/presentation/model/PaymentUiState.kt` |
| `PaymentComposerRepositoryTest` | class | `commonTest` | `feature/payment/src/commonTest/kotlin/com/cbgm/sparrow/feature/payment/data/repository/PaymentComposerRepositoryTest.kt` |
| `SepaPaymentPolicyTest` | class | `commonTest` | `feature/payment/src/commonTest/kotlin/com/cbgm/sparrow/feature/payment/domain/SepaPaymentPolicyTest.kt` |
| `PaymentFixture` | class | `commonTest` | `feature/payment/src/commonTest/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/PaymentSessionUseCasesTest.kt` |
| `PaymentSessionUseCasesTest` | class | `commonTest` | `feature/payment/src/commonTest/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/PaymentSessionUseCasesTest.kt` |
| `TestBankDetailsRepository` | class | `commonTest` | `feature/payment/src/commonTest/kotlin/com/cbgm/sparrow/feature/payment/domain/usecase/PaymentSessionUseCasesTest.kt` |
| `IosBankDetailsStorage` | class | `iosMain` | `feature/payment/src/iosMain/kotlin/com/cbgm/sparrow/feature/payment/device/IosBankDetailsStorage.kt` |

### `:core:base`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `EncryptedBlobReferenceDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/blob/data/model/EncryptedBlobReferenceDto.kt` |
| `GroupMetadataPolicy` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/group/domain/model/GroupMetadataPolicy.kt` |
| `GroupDeliveryStatusAggregation` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/message/domain/model/GroupDeliveryStatusAggregation.kt` |
| `MessageContentStatus` | enum class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/message/domain/model/MessageContentStatus.kt` |
| `MessageDeliveryStatus` | enum class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/message/domain/model/MessageDeliveryStatus.kt` |
| `MessageTransportMode` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/message/domain/model/MessageTransportMode.kt` |
| `ContactDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `ExpenseAllocationDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `ExpenseBoardClosedDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `ExpenseBoardDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `ExpenseConversionDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `ExpenseDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `FileDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `ImageDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `LocationDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `MessagePartDto` | interface | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `PollDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `PollOptionDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `TextDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `VideoDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `VoiceDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/MessagePartDto.kt` |
| `PaymentTransferDto` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/data/model/PaymentTransferDto.kt` |
| `ExpenseMemberId` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/ExpenseMemberId.kt` |
| `ExpensePolicy` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/ExpensePolicy.kt` |
| `ExpenseSplit` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/ExpenseSplit.kt` |
| `MessageAttachmentPolicy` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessageAttachmentPolicy.kt` |
| `Contact` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `Expense` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `ExpenseAllocation` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `ExpenseBoard` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `ExpenseBoardClosed` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `ExpenseCategory` | enum class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `ExpenseConversion` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `File` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `GroupPin` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `Image` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `Location` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `Message` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `MessagePart` | interface | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `MessagePartSource` | interface | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `Poll` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `PollOption` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `Text` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `Video` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `Voice` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessagePart.kt` |
| `PaymentTransfer` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/PaymentTransfer.kt` |
| `PaymentTransferPolicy` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/PaymentTransfer.kt` |
| `PaymentTransferStatus` | enum class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/PaymentTransfer.kt` |
| `PollPolicy` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/PollPolicy.kt` |
| `ContactUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `ExpenseAllocationUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `ExpenseBoardClosedUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `ExpenseBoardUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `ExpenseUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `FileUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `GroupPin` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `ImageUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `ImageVideoUi` | interface | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `LocationUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `Message` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `MessagePartSourceUi` | interface | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `MessagePartUi` | interface | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `PollOptionUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `PollUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `TextUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `VideoUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `VoiceUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/MessagePartUi.kt` |
| `PaymentTransferUi` | data class | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/model/PaymentTransferUi.kt` |
| `ByteArrayAsBase64Serializer` | object | `commonMain` | `core/base/src/commonMain/kotlin/com/cbgm/sparrow/core/base/serialization/ByteArrayAsBase64Serializer.kt` |
| `GroupDeliveryStatusAggregationTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/message/domain/model/GroupDeliveryStatusAggregationTest.kt` |
| `ExpenseBoardClosedDtoMapperTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/messagepart/data/mapper/ExpenseBoardClosedDtoMapperTest.kt` |
| `ExpensePartDtoMapperTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/messagepart/data/mapper/ExpensePartDtoMapperTest.kt` |
| `MessagePartDtoMapperTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/messagepart/data/mapper/MessagePartDtoMapperTest.kt` |
| `PaymentTransferDtoMapperTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/messagepart/data/mapper/PaymentTransferDtoMapperTest.kt` |
| `ExpensePolicyTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/ExpensePolicyTest.kt` |
| `MessageAttachmentPolicyTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/MessageAttachmentPolicyTest.kt` |
| `PaymentTransferPolicyTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/PaymentTransferPolicyTest.kt` |
| `PollPolicyTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/messagepart/domain/model/PollPolicyTest.kt` |
| `MessagePartUiMapperTest` | class | `commonTest` | `core/base/src/commonTest/kotlin/com/cbgm/sparrow/core/base/messagepart/ui/mapper/MessagePartUiMapperTest.kt` |

### `:core:util`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ByteArrays` | object | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/bytes/ByteArrays.kt` |
| `ApplicationCoroutineScope` | class | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/coroutines/ApplicationCoroutineScope.kt` |
| `IdGenerator` | object | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/id/IdGenerator.kt` |
| `ChatOpenTrace` | object | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/logging/ChatOpenTrace.kt` |
| `SparrowErrorSink` | interface | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/logging/SparrowErrorSink.kt` |
| `KermitSparrowLogger` | class | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/logging/SparrowLogger.kt` |
| `PendingError` | data class | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/logging/SparrowLogger.kt` |
| `SparrowLog` | object | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/logging/SparrowLogger.kt` |
| `SparrowLogger` | interface | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/logging/SparrowLogger.kt` |
| `StartupTrace` | object | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/logging/StartupTrace.kt` |
| `DefaultPhoneNumberNormalizer` | class | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/phone/DefaultPhoneNumberNormalizer.kt` |
| `LocalPhoneNumberProvider` | interface | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/phone/LocalPhoneNumberProvider.kt` |
| `PhoneNumberNormalizer` | interface | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/phone/PhoneNumberNormalizer.kt` |
| `SystemClock` | object | `commonMain` | `core/util/src/commonMain/kotlin/com/cbgm/sparrow/core/util/time/SystemClock.kt` |
| `SparrowLogFeedbackTest` | class | `commonTest` | `core/util/src/commonTest/kotlin/com/cbgm/sparrow/core/util/logging/SparrowLogFeedbackTest.kt` |
| `ExpectedReview` | class | `commonTest` | `core/util/src/commonTest/kotlin/com/cbgm/sparrow/core/util/result/SafeCallTest.kt` |
| `SafeCallTest` | class | `commonTest` | `core/util/src/commonTest/kotlin/com/cbgm/sparrow/core/util/result/SafeCallTest.kt` |

### `:core:crypto`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `SodiumIdentityKeyGeneratorTest` | class | `androidDeviceTest` | `core/crypto/src/androidDeviceTest/kotlin/com/cbgm/sparrow/core/crypto/identity/SodiumKeyGeneratorTest.kt` |
| `SodiumTransportMessageCipherTest` | class | `androidDeviceTest` | `core/crypto/src/androidDeviceTest/kotlin/com/cbgm/sparrow/core/crypto/transport/SodiumTransportMessageCipherTest.kt` |
| `TestEncryptionKeyPair` | class | `androidDeviceTest` | `core/crypto/src/androidDeviceTest/kotlin/com/cbgm/sparrow/core/crypto/transport/SodiumTransportMessageCipherTest.kt` |
| `InitializeCryptoRuntime` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/InitializeCryptoRuntime.kt` |
| `SodiumRuntime` | object | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/SodiumRuntime.kt` |
| `BlobCipher` | interface | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/blob/BlobCipher.kt` |
| `EncryptedBlob` | data class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/blob/BlobCipher.kt` |
| `SodiumBlobCipher` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/blob/SodiumBlobCipher.kt` |
| `CryptoException` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `CryptoNotInitializedException` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `InvalidPrivateKeyException` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `InvalidPublicKeyException` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `MessageDecryptionException` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `MessageEncryptionException` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `UnsupportedCryptoVersionException` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `SignatureVerificationException` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/SignatureVerificationException.kt` |
| `CryptoHash` | interface | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/hash/CryptoHash.kt` |
| `DefaultCryptoHash` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/hash/DefaultCryptoHash.kt` |
| `SecureRandomGenerator` | interface | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/random/SecureRandomGenerator.kt` |
| `SodiumSecureRandomGenerator` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/random/SodiumSecureRandomGenerator.kt` |
| `DetachedSignatureCrypto` | interface | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/signature/DetachedSignatureCrypto.kt` |
| `SodiumDetachedSignatureCrypto` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/signature/SodiumDetachedSignatureCrypto.kt` |
| `DecodedTransportMessage` | interface | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DecodedTransportMessage.kt` |
| `DecryptionFailed` | data class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DecodedTransportMessage.kt` |
| `InvalidPacket` | data class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DecodedTransportMessage.kt` |
| `InvalidPlaintext` | data class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DecodedTransportMessage.kt` |
| `Readable` | data class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DecodedTransportMessage.kt` |
| `DefaultIncomingTransportMessageDecoder` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DefaultIncomingTransportMessageDecoder.kt` |
| `DefaultTransportPayloadCodec` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DefaultTransportPayloadCodec.kt` |
| `EncryptedTransportPayload` | data class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/EncryptedTransportPayload.kt` |
| `IncomingTransportMessageDecoder` | interface | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/IncomingTransportMessageDecoder.kt` |
| `SodiumTransportMessageCipher` | class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/SodiumTransportMessageCipher.kt` |
| `TransportEncryptionMode` | enum class | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/TransportEncryptionMode.kt` |
| `TransportMessageCipher` | interface | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/TransportMessageCipher.kt` |
| `TransportPayloadCodec` | interface | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/TransportPayloadCodec.kt` |
| `DefaultTransportPayloadCodecTest` | class | `commonTest` | `core/crypto/src/commonTest/kotlin/com/cbgm/sparrow/core/crypto/transport/DefaultTransportPayloadCodecTest.kt` |

### `:protocol`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `GroupPinnedAttachmentProvider` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/attachment/GroupPinnedAttachmentProvider.kt` |
| `MessageAttachmentType` | enum class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/attachment/MessageAttachmentType.kt` |
| `DirectChatAuthorizationRevocationProtocol` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/authorization/DirectChatAuthorizationRevocationProtocol.kt` |
| `DirectChatAuthorizationRevocationSender` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/authorization/DirectChatAuthorizationRevocationSender.kt` |
| `GroupAvatarMetadata` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/avatar/GroupAvatarMetadata.kt` |
| `GroupAvatarPayload` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/avatar/GroupAvatarMetadata.kt` |
| `GroupAvatarProvider` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/avatar/GroupAvatarProvider.kt` |
| `GroupAvatarSnapshot` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/avatar/GroupAvatarProvider.kt` |
| `KotlinxPacketCodec` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/codec/KotlinxPacketCodec.kt` |
| `PacketCodec` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/codec/PacketCodec.kt` |
| `DefaultProtocolPacketHandler` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/handler/DefaultProtocolPacketHandler.kt` |
| `IncomingMessageHandler` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/handler/IncomingMessageHandler.kt` |
| `IncomingMessageRejectedException` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/handler/IncomingMessageRejectedException.kt` |
| `IncomingPacketContext` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/handler/IncomingPacketContext.kt` |
| `ProtocolPacketHandler` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/handler/ProtocolPacketHandler.kt` |
| `TypedProtocolPacketHandler` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/handler/TypeProtocolHandler.kt` |
| `LocalEncryptionKeyPair` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/identity/LocalEncryptionKeyPair.kt` |
| `LocalEncryptionKeyPairProvider` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/identity/LocalEncryptionKeyPairProvider.kt` |
| `LocalIdentityChangeHandler` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/identity/LocalIdentityChangeHandler.kt` |
| `LocalIdentityUnavailableException` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/identity/LocalIdentityUnavailableException.kt` |
| `LocalPublicIdentity` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/identity/LocalPublicIdentity.kt` |
| `LocalPublicIdentityProvider` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/identity/LocalPublicIdentityProvider.kt` |
| `LocalSigningKeyPair` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/identity/LocalSigningKeyPair.kt` |
| `LocalSigningKeyPairProvider` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/identity/LocalSigningKeyPairProvider.kt` |
| `LocalSigningPublicKeyProvider` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/identity/LocalSigningPublicKeyProvider.kt` |
| `ContactInvitationDeclineProtocol` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationDeclineProtocol.kt` |
| `ContactInvitationHandshakeProtocol` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationHandshakeProtocol.kt` |
| `ContactInvitationPayloadEncoder` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationPayloadEncoder.kt` |
| `ContactInvitationRequestProtocol` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationRequestProtocol.kt` |
| `LocalMailboxCredential` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/mailbox/MailboxModels.kt` |
| `MailboxCapabilityLifecycle` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/mailbox/MailboxModels.kt` |
| `MailboxDeliveryRoute` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/mailbox/MailboxModels.kt` |
| `MailboxRouteRepository` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/mailbox/MailboxModels.kt` |
| `NoOpMailboxCapabilityLifecycle` | object | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/mailbox/MailboxModels.kt` |
| `GroupMessageContent` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/GroupMessageContent.kt` |
| `GroupMessageContentCodec` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/GroupMessageContent.kt` |
| `Delete` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/OperationMessage.kt` |
| `Edit` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/OperationMessage.kt` |
| `MessageOperation` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/OperationMessage.kt` |
| `OperationMessage` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/OperationMessage.kt` |
| `OperationMessageCodec` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/OperationMessage.kt` |
| `PollClose` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/OperationMessage.kt` |
| `PollVote` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/OperationMessage.kt` |
| `Reaction` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/message/OperationMessage.kt` |
| `OutboxDeliveryStateListener` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/OutboxDeliveryStateListener.kt` |
| `OutboxEvent` | enum class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/OutboxEvent.kt` |
| `OutboxProcessingResult` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/OutboxProcessor.kt` |
| `OutboxProcessor` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/OutboxProcessor.kt` |
| `OutboxRunner` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/OutboxRunner.kt` |
| `OutboxStateMachine` | object | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/OutboxStateMachine.kt` |
| `OutboxStatus` | enum class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/OutboxStatus.kt` |
| `ProtocolOutbox` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/ProtocolOutbox.kt` |
| `ProtocolOutboxFailureEvent` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/ProtocolOutboxFailureEvent.kt` |
| `ProtocolOutboxItem` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/outbox/ProtocolOutboxItem.kt` |
| `ChatMessagePacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/ChatMessagePacket.kt` |
| `ContactInviteAcceptedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/ContactInvitationPackets.kt` |
| `ContactInviteDeclinedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/ContactInvitationPackets.kt` |
| `ContactInvitePacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/ContactInvitationPackets.kt` |
| `ContactReadyPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/ContactInvitationPackets.kt` |
| `ContactVerificationReceiptPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/ContactInvitationPackets.kt` |
| `DirectChatAuthorizationRevokedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/ContactInvitationPackets.kt` |
| `DeliveryReceiptPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/DeliveryReceiptPacket.kt` |
| `GroupAvatarUpdatedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupAvatarUpdatedPacket.kt` |
| `GroupChatMessagePacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupChatMessagePacket.kt` |
| `GroupConversationDeletedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupConversationDeletedPacket.kt` |
| `GroupCreatedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupCreatedPacket.kt` |
| `GroupMembershipChangePayload` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupCreatedPacket.kt` |
| `GroupDescriptionUpdatedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupDescriptionUpdatedPacket.kt` |
| `GroupInviteDeclinedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupInviteDeclinedPacket.kt` |
| `GroupInvitePacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupInvitePacket.kt` |
| `GroupInviteReceivedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupInviteReceivedPacket.kt` |
| `GroupJoinRequestPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupJoinRequestPacket.kt` |
| `GroupLeaveRequestPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupLeaveRequestPacket.kt` |
| `GroupMemberActivatedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupMemberActivatedPacket.kt` |
| `GroupMemberActivationAcknowledgementPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupMemberActivationAcknowledgementPacket.kt` |
| `GroupMemberPayload` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupMemberPayload.kt` |
| `GroupMemberRemovedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupMemberRemovedPacket.kt` |
| `GroupPinUpdatedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupPinUpdatedPacket.kt` |
| `GroupProtocolPayloadEncoder` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupProtocolPayloadEncoder.kt` |
| `GroupReadyAcknowledgementPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupReadyAcknowledgementPacket.kt` |
| `GroupTitleUpdatedPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupTitleUpdatedPacket.kt` |
| `GroupVerificationMemberPayload` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupVerificationPackets.kt` |
| `GroupVerificationReceiptPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupVerificationPackets.kt` |
| `GroupVerificationSnapshotPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupVerificationPackets.kt` |
| `GroupVerificationSnapshotRequestPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/GroupVerificationPackets.kt` |
| `IdentityAcknowledgementPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/IdentityAcknowledgementPacket.kt` |
| `IdentityPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/IdentityPacket.kt` |
| `MailboxRoutePacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/MailboxRoutePacket.kt` |
| `OperationMessagePacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/OperationMessagePacket.kt` |
| `ReadReceiptPacket` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/ReadReceiptPacket.kt` |
| `SparrowPacket` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/packet/SparrowPacket.kt` |
| `LocalProfilePictureMetadataProvider` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/profile/LocalProfilePictureMetadataProvider.kt` |
| `LocalProfilePictureProvider` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/profile/LocalProfilePictureProvider.kt` |
| `LocalProfilePictureSnapshot` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/profile/LocalProfilePictureProvider.kt` |
| `ProfilePictureMetadata` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/profile/ProfilePictureMetadata.kt` |
| `ProfilePicturePayload` | class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/profile/ProfilePictureMetadata.kt` |
| `RemoteProfilePictureMetadataProcessor` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/profile/RemoteProfilePictureMetadataProcessor.kt` |
| `RemoteProfilePictureProvider` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/profile/RemoteProfilePictureProvider.kt` |
| `RemoteProfilePictureSnapshot` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/profile/RemoteProfilePictureProvider.kt` |
| `OutgoingWireAcceptance` | data class | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/transport/OutgoingWireSender.kt` |
| `OutgoingWireSender` | interface | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/transport/OutgoingWireSender.kt` |
| `ProtocolVersion` | object | `commonMain` | `protocol/src/commonMain/kotlin/com/cbgm/sparrow/protocol/version/ProtocolVersion.kt` |
| `DirectChatAuthorizationRevocationProtocolTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/authorization/DirectChatAuthorizationRevocationProtocolTest.kt` |
| `RecordingCrypto` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/authorization/DirectChatAuthorizationRevocationProtocolTest.kt` |
| `DirectChatAuthorizationRevocationSenderTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/authorization/DirectChatAuthorizationRevocationSenderTest.kt` |
| `RecordingOutbox` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/authorization/DirectChatAuthorizationRevocationSenderTest.kt` |
| `GroupAvatarPacketCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/avatar/GroupAvatarPacketCodecTest.kt` |
| `ContactInvitationPacketCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/codec/ContactInvitationPacketCodecTest.kt` |
| `GroupChatMessagePacketCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/codec/GroupChatMessagePacketCodecTest.kt` |
| `GroupCreatedPacketCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/codec/GroupCreatedPacketCodecTest.kt` |
| `GroupInvitationPacketCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/codec/GroupInvitationPacketCodecTest.kt` |
| `GroupVerificationPacketCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/codec/GroupVerificationPacketCodecTest.kt` |
| `KotlinxPacketCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/codec/KotlinxPacketCodecTest.kt` |
| `MailboxRoutePacketCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/codec/MailboxRoutePacketCodecTest.kt` |
| `DefaultProtocolPacketHandlerTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/handler/DefaultProtocolPacketHandlerTest.kt` |
| `RecordingHandler` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/handler/DefaultProtocolPacketHandlerTest.kt` |
| `ContactInvitationDeclineProtocolTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationDeclineProtocolTest.kt` |
| `RecordingCrypto` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationDeclineProtocolTest.kt` |
| `ContactInvitationPacketCreationTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationPacketCreationTest.kt` |
| `RecordingCrypto` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationPacketCreationTest.kt` |
| `ContactInvitationPayloadEncoderTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationPayloadEncoderTest.kt` |
| `ContactInvitationRequestProtocolTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationRequestProtocolTest.kt` |
| `RecordingCrypto` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/invitation/ContactInvitationRequestProtocolTest.kt` |
| `GroupMessageContentCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/message/GroupMessageContentCodecTest.kt` |
| `GroupPollContentTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/message/GroupPollContentTest.kt` |
| `ExpenseWireValidationTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/messagepart/ExpenseWireValidationTest.kt` |
| `PaymentWireValidationTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/messagepart/PaymentWireValidationTest.kt` |
| `OutboxStateMachineTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/outbox/OutboxStateMachineTest.kt` |
| `ProfilePictureMetadataCodecTest` | class | `commonTest` | `protocol/src/commonTest/kotlin/com/cbgm/sparrow/protocol/profile/ProfilePictureMetadataCodecTest.kt` |

### `:feature:messaging`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `MessagingFailureEvent` | data class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingFailureEvent.kt` |
| `MessagingIndicator` | data class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingIndicator.kt` |
| `MessagingTransportResult` | data class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingTransportResult.kt` |
| `MessagingTransportState` | enum class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingTransportResult.kt` |
| `AcknowledgeMessagingFailureUseCase` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/AcknowledgeMessagingFailureUseCase.kt` |
| `ObserveMessagingFailureEventsUseCase` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingFailureEventsUseCase.kt` |
| `ObserveMessagingIndicatorsUseCase` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingIndicatorsUseCase.kt` |
| `ObserveMessagingTransportResultsUseCase` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingTransportResultsUseCase.kt` |
| `SendEncodedTransportUseCase` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/SendEncodedTransportUseCase.kt` |
| `SendMessagingIndicatorUseCase` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/SendMessagingIndicatorUseCase.kt` |
| `DefaultIncomingEnvelopeRunner` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/DefaultIncomingEnvelopeRunner.kt` |
| `IncomingEnvelopeGateway` | interface | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeGateway.kt` |
| `IncomingTransportEnvelope` | data class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeGateway.kt` |
| `IncomingEnvelopeProcessingResult` | enum class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeProcessor.kt` |
| `IncomingEnvelopeProcessor` | interface | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeProcessor.kt` |
| `IncomingEnvelopeRunner` | interface | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeRunner.kt` |
| `MessagingIndicatorGateway` | interface | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/indicator/MessagingIndicatorGateway.kt` |
| `MailboxCoordinator` | interface | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/mailbox/MailboxCoordinator.kt` |
| `MailboxRoutePayloadEncoder` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/mailbox/MailboxRoutePayloadEncoder.kt` |
| `DefaultOutboxProcessor` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxProcessor.kt` |
| `DefaultOutboxRunner` | class | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxRunner.kt` |
| `FakeGateway` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/MessagingIndicatorUseCasesTest.kt` |
| `MessagingIndicatorUseCasesTest` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/MessagingIndicatorUseCasesTest.kt` |
| `FakeOutbox` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingFailureEventsUseCaseTest.kt` |
| `ObserveMessagingFailureEventsUseCaseTest` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingFailureEventsUseCaseTest.kt` |
| `FakeOutbox` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingTransportResultsUseCaseTest.kt` |
| `ObserveMessagingTransportResultsUseCaseTest` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingTransportResultsUseCaseTest.kt` |
| `FakeOutgoingWireSender` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/SendEncodedTransportUseCaseTest.kt` |
| `SendEncodedTransportUseCaseTest` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/SendEncodedTransportUseCaseTest.kt` |
| `DefaultIncomingEnvelopeRunnerTest` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/DefaultIncomingEnvelopeRunnerTest.kt` |
| `FakeIncomingEnvelopeGateway` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/DefaultIncomingEnvelopeRunnerTest.kt` |
| `DefaultOutboxProcessorTest` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxProcessorTest.kt` |
| `RecordingDeliveryListener` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxProcessorTest.kt` |
| `RecordingOutbox` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxProcessorTest.kt` |
| `DefaultOutboxRunnerTest` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxRunnerTest.kt` |
| `FakeProtocolOutbox` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxRunnerTest.kt` |
| `RecordingOutboxProcessor` | class | `commonTest` | `feature/messaging/src/commonTest/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxRunnerTest.kt` |

### `:feature:transport`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ControlPlaneReaddAfterRemoveTest` | class | `androidDeviceTest` | `feature/transport/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneReaddAfterRemoveTest.kt` |
| `ControlPlaneConfiguration` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneDirectorySynchronizer` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneEndpoint` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneEndpointStatus` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneHealthMonitor` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneReachability` | enum class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/ControlPlaneConfiguration.kt` |
| `ControlPlaneStatusStore` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/ControlPlaneConfiguration.kt` |
| `TransportDiagnosticConnectionState` | enum class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/TransportDiagnostics.kt` |
| `TransportDiagnostics` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/TransportDiagnostics.kt` |
| `TransportDiagnosticsProvider` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/TransportDiagnostics.kt` |
| `TransportNodeDiagnostic` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/TransportDiagnostics.kt` |
| `TransportNodeDiagnosticState` | enum class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/TransportDiagnostics.kt` |
| `TransportConfig` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/config/TransportConfig.kt` |
| `DefaultTransportConnectionManager` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/DefaultTransportConnectionManager.kt` |
| `TransportConnectionManager` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionManager.kt` |
| `Connected` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionState.kt` |
| `Connecting` | object | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionState.kt` |
| `Disconnected` | object | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionState.kt` |
| `Failed` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionState.kt` |
| `TransportConnectionState` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionState.kt` |
| `MissingEndpoint` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportDiagnosticsState.kt` |
| `TransportDiagnosticsState` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportDiagnosticsState.kt` |
| `ControlPlaneCandidateVerifier` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneCandidateVerifier.kt` |
| `SignedDirectoryControlPlaneCandidateVerifier` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneCandidateVerifier.kt` |
| `ControlPlaneConfigurationImpl` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneConfigurationImpl.kt` |
| `ControlPlaneDirectoryRemoteSource` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneDirectoryRemoteSource.kt` |
| `SignedDirectoryDownload` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneDirectoryRemoteSource.kt` |
| `ControlPlaneRequestRejectedException` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestExceptions.kt` |
| `ControlPlaneUnavailableException` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestExceptions.kt` |
| `ControlPlaneRequestRouter` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestRouter.kt` |
| `HttpControlPlaneDirectorySynchronizer` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/HttpControlPlaneDirectorySynchronizer.kt` |
| `HttpControlPlaneHealthMonitor` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/HttpControlPlaneHealthMonitor.kt` |
| `HttpNodeControlPlaneDirectorySource` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDirectorySource.kt` |
| `NodeControlPlaneDirectory` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDirectorySource.kt` |
| `NodeControlPlaneDirectorySource` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDirectorySource.kt` |
| `NodeControlPlaneDiscoverySynchronizer` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDiscoverySynchronizer.kt` |
| `DirectoryEnvelope` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `DirectoryPayload` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `DirectoryPlane` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `SignedControlPlaneDirectoryVerifier` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `StaleDirectoryPlane` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/VerifiedControlPlaneDirectoryState.kt` |
| `VerifiedControlPlaneDirectoryState` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/VerifiedControlPlaneDirectoryState.kt` |
| `DataStoreNodeDirectoryCache` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/DataStoreNodeDirectoryCache.kt` |
| `DefaultNodeEndpointResolver` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/DefaultNodeEndpointResolver.kt` |
| `FailedNodeTracker` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/FailedNodeTracker.kt` |
| `CachedNodeDirectory` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryCache.kt` |
| `NodeDirectoryCache` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryCache.kt` |
| `NodeCapability` | enum class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `NodeDirectory` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `NodeEndpoint` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `RegistryAuthorityCertificate` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `RegistrySigningCertificate` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `SignedNodeDirectory` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `SparrowNodeDescriptor` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `UnsignedNodeDescriptor` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `UnsignedRegistryAuthorityCertificate` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `UnsignedRegistrySigningCertificate` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `HttpNodeDirectorySource` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectorySource.kt` |
| `NodeDirectorySource` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectorySource.kt` |
| `NodeDirectoryVerifier` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryVerifier.kt` |
| `NodeEndpointResolver` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeEndpointResolver.kt` |
| `NodeEndpointSelector` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeEndpointSelector.kt` |
| `ClientRoute` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `ClientRouteRegistration` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `GatewayNodeInformation` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `UnsignedClientRoute` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `FederatedEnvelope` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/FederatedEnvelope.kt` |
| `GatewayBlobUploadTicket` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayBlobUploadTicket.kt` |
| `GatewayBlobUploadTicketRequest` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayBlobUploadTicket.kt` |
| `AcknowledgeEnvelope` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `GatewayClientMessage` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `IndicatorState` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `RefreshRoute` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `Register` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `RequestBlobUploadTicket` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `SendEnvelope` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `SendFederatedEnvelope` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `GatewayEnvelopeAcceptance` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayEnvelopeAcceptance.kt` |
| `GatewayIndicatorEvent` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayIndicatorEvent.kt` |
| `BlobUploadTicketIssued` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `BlobUploadTicketRejected` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `EnvelopeAccepted` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `Error` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `GatewayServerMessage` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `IncomingEnvelope` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `IndicatorState` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `Registered` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `RouteRegistered` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `TransportEnvelope` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/TransportEnvelope.kt` |
| `HttpMailboxGateway` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/HttpMailboxGateway.kt` |
| `CreateMailboxRequest` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `CreateMailboxResponse` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `MailboxEnvelopesResponse` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `MailboxGateway` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `ClientPresenceRouteCoordinator` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `ClientPresenceRouteSession` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `GatewayInformationSnapshot` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `PresenceRouteConnection` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `AnnounceReady` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `AwaitingGatewayRegistration` | object | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteEffect` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteEvent` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteState` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteStateMachine` | object | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteTransition` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `Fail` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `Failed` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `GatewayInformationLoadFailed` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `GatewayInformationLoaded` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `GatewayRegistered` | object | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `LoadGatewayInformation` | object | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `LoadingGatewayInformation` | object | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `PrepareRegistration` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `PreparingRegistration` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `PublishRoute` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `PublishingRoute` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `Ready` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `RefreshDue` | object | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `RegistrationPreparationFailed` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `RegistrationPrepared` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `RouteAccepted` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `RouteExpired` | object | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `RoutePublicationFailed` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `RouteRejected` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ScheduleRefresh` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientRouteRegistrationFactory` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientRouteRegistrationFactory.kt` |
| `PresenceRouteRefreshRejectedException` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/PresenceRouteRefreshRejectedException.kt` |
| `HttpPushTokenRegistrationGateway` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/HttpPushTokenRegistrationGateway.kt` |
| `PushDeviceRegistrationRequest` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/PushModels.kt` |
| `PushPlatform` | enum class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/PushTokenRegistrationGateway.kt` |
| `PushTokenRegistrationGateway` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/PushTokenRegistrationGateway.kt` |
| `HttpPendingEnvelopeGateway` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/inbox/HttpPendingEnvelopeGateway.kt` |
| `PendingEnvelopeGateway` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/inbox/PendingEnvelopeGateway.kt` |
| `PendingTransportEnvelopesResponse` | data class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/inbox/PendingEnvelopeModels.kt` |
| `DefaultLocalBootstrapRoutingIdProvider` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/DefaultLocalBootstrapRoutingIdProvider.kt` |
| `DefaultLocalRoutingIdProvider` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/DefaultLocalRoutingIdProvider.kt` |
| `LocalBootstrapRoutingIdProvider` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/LocalBootstrapRoutingIdProvider.kt` |
| `LocalRoutingIdProvider` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/LocalRoutingIdProvider.kt` |
| `RoutingIdGenerator` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/RoutingIdGenerator.kt` |
| `Sha256RoutingIdGenerator` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/Sha256RoutingIdGenerator.kt` |
| `WebSocketOutgoingWireSender` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/sender/WebSocketOutgoingWireSender.kt` |
| `DefaultWebSocketTransportClient` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/DefaultWebSocketTransportClient.kt` |
| `GatewayPendingRequestRegistry` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/GatewayPendingRequestRegistry.kt` |
| `GatewayServerMessageHandler` | class | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/GatewayServerMessageHandler.kt` |
| `WebSocketTransportClient` | interface | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/WebSocketTransportClient.kt` |
| `TransportConfigTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/config/TransportConfigTest.kt` |
| `ConnectException` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/connection/RecoverableConnectivityFailureTest.kt` |
| `ConnectTimeoutException` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/connection/RecoverableConnectivityFailureTest.kt` |
| `RecoverableConnectivityFailureTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/connection/RecoverableConnectivityFailureTest.kt` |
| `TransportDiagnosticsStateTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportDiagnosticsStateTest.kt` |
| `ControlPlaneRequestRouterTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestRouterTest.kt` |
| `FakeControlPlaneConfiguration` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestRouterTest.kt` |
| `HttpControlPlaneDirectorySynchronizerTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/controlplane/HttpControlPlaneDirectorySynchronizerTest.kt` |
| `FakeControlPlaneCandidateVerifier` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDiscoverySynchronizerTest.kt` |
| `FakeControlPlaneConfiguration` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDiscoverySynchronizerTest.kt` |
| `FakeNodeControlPlaneDirectorySource` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDiscoverySynchronizerTest.kt` |
| `NodeControlPlaneDiscoverySynchronizerTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDiscoverySynchronizerTest.kt` |
| `AcceptingSignatureCrypto` | object | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/DefaultNodeEndpointResolverTest.kt` |
| `DefaultNodeEndpointResolverTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/DefaultNodeEndpointResolverTest.kt` |
| `FakeControlPlaneConfiguration` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/DefaultNodeEndpointResolverTest.kt` |
| `RecordingNodeDirectoryCache` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/DefaultNodeEndpointResolverTest.kt` |
| `RecordingNodeDirectorySource` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/DefaultNodeEndpointResolverTest.kt` |
| `RecordingPerPlaneSource` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/DefaultNodeEndpointResolverTest.kt` |
| `FailedNodeTrackerTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/FailedNodeTrackerTest.kt` |
| `AcceptingSignatureCrypto` | object | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryVerifierTest.kt` |
| `NodeDirectoryVerifierTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryVerifierTest.kt` |
| `RejectingSignatureCrypto` | object | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryVerifierTest.kt` |
| `GatewayClientMessageTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessageTest.kt` |
| `ClientPresenceRouteStateMachineTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachineTest.kt` |
| `ClientRouteRegistrationFactoryTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientRouteRegistrationFactoryTest.kt` |
| `Sha256RoutingIdGeneratorTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/routing/Sha256RoutingIdGeneratorTest.kt` |
| `FakeMailboxRouteRepository` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/sender/WebSocketOutgoingWireSenderTest.kt` |
| `RecordingWebSocketTransportClient` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/sender/WebSocketOutgoingWireSenderTest.kt` |
| `SuccessfulLocalBootstrapRoutingIdProvider` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/sender/WebSocketOutgoingWireSenderTest.kt` |
| `SuccessfulLocalRoutingIdProvider` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/sender/WebSocketOutgoingWireSenderTest.kt` |
| `WebSocketOutgoingWireSenderTest` | class | `commonTest` | `feature/transport/src/commonTest/kotlin/com/cbgm/sparrow/feature/transport/sender/WebSocketOutgoingWireSenderTest.kt` |

### `:feature:onboarding`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AutomaticPhoneNumberResult` | interface | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/device/OnboardingPermissions.kt` |
| `Failed` | data class | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/device/OnboardingPermissions.kt` |
| `Found` | data class | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/device/OnboardingPermissions.kt` |
| `PermissionRequestResult` | data class | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/device/OnboardingPermissions.kt` |
| `Unavailable` | object | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/device/OnboardingPermissions.kt` |
| `OnboardingViewModel` | class | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/OnboardingViewModel.kt` |
| `ChooseAnotherNumberClicked` | object | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiEvent.kt` |
| `NextClicked` | object | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiEvent.kt` |
| `OnboardingUiEvent` | interface | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiEvent.kt` |
| `RequestPermissionsClicked` | object | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiEvent.kt` |
| `RetryAutomaticNumberClicked` | object | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiEvent.kt` |
| `OnboardingPage` | enum class | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiState.kt` |
| `OnboardingUiState` | data class | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/model/OnboardingUiState.kt` |
| `PageData` | data class | `commonMain` | `feature/onboarding/src/commonMain/kotlin/com/cbgm/sparrow/feature/onboarding/presentation/pages/PermissionsPage.kt` |

### `:startup`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AppConnectionAvailability` | enum class | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/model/AppConnectionAvailability.kt` |
| `Error` | data class | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/model/StartupResult.kt` |
| `IdentityRequired` | object | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/model/StartupResult.kt` |
| `Ready` | object | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/model/StartupResult.kt` |
| `StartupResult` | interface | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/model/StartupResult.kt` |
| `StartupRunner` | interface | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/runner/StartupRunner.kt` |
| `ObserveAppConnectionAvailabilityUseCase` | class | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/domain/usecase/ObserveAppConnectionAvailabilityUseCase.kt` |
| `StartupViewModel` | class | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/StartupViewModel.kt` |
| `IdentityCreated` | object | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiEvent.kt` |
| `RetryClicked` | object | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiEvent.kt` |
| `StartupUiEvent` | interface | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiEvent.kt` |
| `Error` | data class | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiState.kt` |
| `IdentityRequired` | object | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiState.kt` |
| `Loading` | object | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiState.kt` |
| `Ready` | object | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiState.kt` |
| `StartupUiState` | interface | `commonMain` | `startup/src/commonMain/kotlin/com/cbgm/sparrow/startup/presentation/start/model/StartupUiState.kt` |

### `:navigation`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `RecoveryInboxViewModel` | class | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/presentation/inbox/RecoveryInboxViewModel.kt` |
| `MainViewModel` | class | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/presentation/main/MainViewModel.kt` |
| `MainTab` | enum class | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/presentation/main/model/MainTab.kt` |
| `FeedbackKind` | enum class | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/routing/AppNavigation.kt` |
| `FeedbackSnackbarVisuals` | data class | `commonMain` | `navigation/src/commonMain/kotlin/com/cbgm/sparrow/navigation/routing/AppNavigation.kt` |

### `:feature:notification`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AndroidNotificationRuntime` | class | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/AndroidNotificationRuntime.kt` |
| `BackgroundDeliveryReceiptSender` | class | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/BackgroundDeliveryReceiptSender.kt` |
| `PendingMessageSyncScheduler` | class | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/PendingMessageSyncScheduler.kt` |
| `PendingMessageSyncWorker` | class | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/PendingMessageSyncWorker.kt` |
| `PushTokenRegistrationScheduler` | class | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/PushTokenRegistrationScheduler.kt` |
| `PushTokenRegistrationWorker` | class | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/PushTokenRegistrationWorker.kt` |
| `SparrowDeepLink` | object | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/SparrowDeepLink.kt` |
| `SparrowFirebaseMessagingService` | class | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/SparrowFirebaseMessagingService.kt` |
| `SparrowNotificationIntentFactory` | object | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/SparrowNotificationIntentFactory.kt` |
| `SparrowNotificationIntentHandler` | class | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/SparrowNotificationIntentHandler.kt` |
| `SparrowNotificationManager` | class | `androidMain` | `feature/notification/src/androidMain/kotlin/com/cbgm/sparrow/feature/notification/device/SparrowNotificationManager.kt` |
| `PlatformNotificationRuntime` | interface | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/device/PlatformNotificationRuntime.kt` |
| `AppVisibilityState` | class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/model/AppVisibilityState.kt` |
| `ConversationNotification` | data class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/model/ConversationNotification.kt` |
| `Cancel` | data class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/model/ConversationNotificationEvent.kt` |
| `ConversationNotificationEvent` | interface | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/model/ConversationNotificationEvent.kt` |
| `Show` | data class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/model/ConversationNotificationEvent.kt` |
| `Direct` | data class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/model/NotificationConversationTarget.kt` |
| `Group` | data class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/model/NotificationConversationTarget.kt` |
| `NotificationConversationTarget` | interface | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/model/NotificationConversationTarget.kt` |
| `PendingMessageSyncResult` | data class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/model/PendingMessageSyncResult.kt` |
| `ObserveConversationNotificationEventsUseCase` | class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/usecase/ObserveConversationNotificationEventsUseCase.kt` |
| `RegisterPushTokenUseCase` | class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/usecase/RegisterPushTokenUseCase.kt` |
| `ResolveNotificationConversationUseCase` | class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/usecase/ResolveNotificationConversationUseCase.kt` |
| `SynchronizePendingMessagesUseCase` | class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/domain/usecase/SynchronizePendingMessagesUseCase.kt` |
| `ConversationNotificationCoordinator` | class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/presentation/ConversationNotificationCoordinator.kt` |
| `ConversationNotificationPresenter` | interface | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/presentation/ConversationNotificationPresenter.kt` |
| `NotificationNavigationController` | class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/presentation/navigation/NotificationNavigationController.kt` |
| `Conversation` | data class | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/presentation/navigation/NotificationNavigationTarget.kt` |
| `NotificationNavigationTarget` | interface | `commonMain` | `feature/notification/src/commonMain/kotlin/com/cbgm/sparrow/feature/notification/presentation/navigation/NotificationNavigationTarget.kt` |
| `NotificationNavigationControllerTest` | class | `commonTest` | `feature/notification/src/commonTest/kotlin/com/cbgm/sparrow/feature/notification/presentation/navigation/NotificationNavigationControllerTest.kt` |

### `:core:ui`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `BlockScreenshotFlagManager` | object | `androidMain` | `core/ui/src/androidMain/kotlin/com/cbgm/sparrow/core/ui/component/BlockScreenshotEffect.android.kt` |
| `SecureWindowState` | data class | `androidMain` | `core/ui/src/androidMain/kotlin/com/cbgm/sparrow/core/ui/component/BlockScreenshotEffect.android.kt` |
| `LocalAppLocale` | object | `androidMain` | `core/ui/src/androidMain/kotlin/com/cbgm/sparrow/core/ui/locale/LocaleAppLocale.android.kt` |
| `FeedbackOverlayData` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/AnchoredFeedback.kt` |
| `PatternElement` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/PatternBackgrund.kt` |
| `LazyList` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/Scaffold.kt` |
| `Scroll` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/Scaffold.kt` |
| `SparrowScrollState` | interface | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/Scaffold.kt` |
| `SparrowScrollStateType` | enum class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/Scaffold.kt` |
| `SparrowTabbedScrollStates` | class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/Scaffold.kt` |
| `SparrowOverlayAnchor` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/SparrowOverlay.kt` |
| `SparrowOverlayScope` | interface | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/SparrowOverlay.kt` |
| `SparrowOverlayScopeImpl` | class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/SparrowOverlay.kt` |
| `ParsedSparrowPath` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/StartupArtwork.kt` |
| `SparrowPathSpec` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/StartupArtwork.kt` |
| `SwipeRevealAction` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/component/SwipeRevealActions.kt` |
| `ClipboardWriter` | interface | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/device/clipboard/ClipboardWriter.kt` |
| `BorderSides` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/helper/Border.kt` |
| `AppLanguage` | enum class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/locale/AppLanguage.kt` |
| `LocalAppLocale` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/locale/LocalAppLocale.kt` |
| `AppNavigationEvent` | interface | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppNavigationEvent.kt` |
| `NavigateTo` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppNavigationEvent.kt` |
| `PopBackStack` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppNavigationEvent.kt` |
| `PopBackStackTo` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppNavigationEvent.kt` |
| `AppNavigator` | class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppNavigator.kt` |
| `AppRoute` | interface | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `AttachmentManagement` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `AttachmentStorage` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `AutoReplySettings` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `BlockedContacts` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `Chat` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `ContactDetails` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `ControlPlanes` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `CreateExpense` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `CreatePoll` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `DataDisclaimer` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `DeveloperErrorLog` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `DeveloperMenu` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `DeveloperNodes` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `ExpenseDetail` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `ExpenseOverview` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `FilePicker` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `GroupConversation` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `GroupDetails` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `IdentityBackup` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `IdentityKeys` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `ImportContact` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `Invitations` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `Licenses` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `Main` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `MessageSafetyDetails` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `MessageSearch` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `Payment` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `PrivacyPolicy` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `ScanIdentity` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `ShareIdentity` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `Startup` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `VerifyIdentityQr` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/navigation/AppRoute.kt` |
| `BaseViewModel` | class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/presentation/BaseViewModel.kt` |
| `BarsState` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/scroll/BarsState.kt` |
| `Alpha` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `Badge` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `BlockedContactsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `ContactDetailsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `ContactsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `DirectScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `DisclaimerScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `FloatingButton` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `GroupDetailsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `GroupScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `IdentityScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `ImportIdentityScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `MediaSelection` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `MessageInput` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `NetworkDiagnosticsCard` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `OverviewScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `PatternBackground` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `PermissionsPage` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `ProfileSettingsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `ScanIdentityScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `SettingsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `ShareIdentityScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `StartupScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `TextField` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Alpha.kt` |
| `AttachmentColors` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/AttachmentColors.kt` |
| `Colors` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Colors.kt` |
| `FunctionalColors` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Colors.kt` |
| `ActionMenu` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `AppLockScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `AttachmentBar` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Avatar` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Badge` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Base` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `BlockedContactsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Button` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Card` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `ContactDetailsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `ContactsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `ControlPlaneSettingsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `DeveloperMenuScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Dimens` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `DirectConversationScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Expense` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `GroupConversationScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `GroupDetailsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `IdentityScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `IdentityVerificationScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `ImportIdentityScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `InvitationsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MainScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Media` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MediaSelection` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MessageAttachment` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MessageBubble` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MessageContext` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MessageDeletion` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MessageInput` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MessageList` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MessageReaction` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `MessageSafetyWarning` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `OnboardingScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `OverviewScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `PatternBackground` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Payment` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `PermissionsPage` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `Poll` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `SearchField` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `SettingsScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `ShareIdentityScreen` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `SwipeRevealActions` | object | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Dimens.kt` |
| `ContactDetailsScreenShapes` | class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `ContactsScreenShapes` | class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `MessageBubbleShapes` | class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `MessageInputShapes` | class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `ScanIdentityScreenShapes` | class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Shapes.kt` |
| `ActionItem` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `ButtonSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `CardSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `ContactsScreenSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `DirectConversationScreenSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `FieldSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `GroupConversationScreenSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `GroupDetailsScreenSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `IdentityScreenSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `ImportIdentityScreenSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `MessageBubbleSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `MessageListSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `OverviewScreenSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `Spacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `StartupScreenSpacing` | data class | `commonMain` | `core/ui/src/commonMain/kotlin/com/cbgm/sparrow/core/ui/theme/Spacing.kt` |
| `AppNavigatorTest` | class | `commonTest` | `core/ui/src/commonTest/kotlin/com/cbgm/sparrow/core/ui/navigation/AppNavigatorTest.kt` |

### `:feature:settings`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `AndroidBuildInfoProvider` | class | `androidMain` | `feature/settings/src/androidMain/kotlin/com/cbgm/sparrow/feature/settings/device/AndroidBuildInfoProvider.kt` |
| `AndroidSystemLanguageProvider` | class | `androidMain` | `feature/settings/src/androidMain/kotlin/com/cbgm/sparrow/feature/settings/device/AndroidSystemLanguageProvider.kt` |
| `Clear` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/DeveloperErrorLogStorageDataSource.kt` |
| `Command` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/DeveloperErrorLogStorageDataSource.kt` |
| `DeveloperErrorLogStorageDataSource` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/DeveloperErrorLogStorageDataSource.kt` |
| `Record` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/DeveloperErrorLogStorageDataSource.kt` |
| `InMemorySettingsStorage` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/InMemorySettingsStorage.kt` |
| `SettingsStorage` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/SettingsStorage.kt` |
| `SettingsStorageImpl` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/datasource/SettingsStorageImpl.kt` |
| `DeveloperErrorDto` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/model/DeveloperErrorDto.kt` |
| `ContactBlocklistRepositoryImpl` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/ContactBlocklistRepositoryImpl.kt` |
| `DeveloperErrorLogRepositoryImpl` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/DeveloperErrorLogRepositoryImpl.kt` |
| `DirectIdentitySetupModeRepositoryImpl` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/DirectIdentitySetupModeRepositoryImpl.kt` |
| `LicencesRepositoryImpl` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/LicencesRepositoryImpl.kt` |
| `SettingsRepositoryImpl` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/data/repository/SettingsRepositoryImpl.kt` |
| `BuildInfoProvider` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/device/BuildInfoProvider.kt` |
| `SystemLanguageProvider` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/device/SystemLanguageProvider.kt` |
| `BuildInfo` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/BuildInfo.kt` |
| `ControlPlaneSettingsContext` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/ControlPlaneSettingsContext.kt` |
| `DeveloperError` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/DeveloperError.kt` |
| `DisclaimerContent` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/DisclaimerContent.kt` |
| `SettingsDomainContext` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/model/SettingsDomainContext.kt` |
| `DeveloperErrorLogRepository` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/repository/DeveloperErrorLogRepository.kt` |
| `LicensesRepository` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/repository/LicensesRepository.kt` |
| `SettingsRepository` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/repository/SettingsRepository.kt` |
| `ClearDeveloperErrorsUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ClearDeveloperErrorsUseCase.kt` |
| `ClearLocalDataUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ClearLocalDataUseCase.kt` |
| `GetAppLanguageUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetAppLanguageUseCase.kt` |
| `GetBuildInfoUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetBuildInfoUseCase.kt` |
| `GetDeveloperEnabledUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetDeveloperEnabledUseCase.kt` |
| `GetLicensesUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/GetLicensesUseCase.kt` |
| `InitAppLanguageUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/InitAppLanguageUseCase.kt` |
| `ObserveBlockUnknownContactInvitesUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveBlockUnknownContactInvitesUseCase.kt` |
| `ObserveBlockedContactIdsUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveBlockedContactIdsUseCase.kt` |
| `ObserveControlPlaneSettingsContextUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveControlPlaneSettingsContextUseCase.kt` |
| `ObserveDeveloperErrorsUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveDeveloperErrorsUseCase.kt` |
| `ObserveDirectIdentitySetupModeUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveDirectIdentitySetupModeUseCase.kt` |
| `LocalAiContext` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveSettingsDomainContextUseCase.kt` |
| `ObserveSettingsDomainContextUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveSettingsDomainContextUseCase.kt` |
| `PrivacyContext` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/ObserveSettingsDomainContextUseCase.kt` |
| `SetAppLanguageUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetAppLanguageUseCase.kt` |
| `SetBlockUnknownContactInvitesUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetBlockUnknownContactInvitesUseCase.kt` |
| `SetDeveloperEnabledUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetDeveloperEnabledUseCase.kt` |
| `SetDirectIdentitySetupModeUseCase` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/domain/usecase/SetDirectIdentitySetupModeUseCase.kt` |
| `DeveloperMenuViewModel` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/DeveloperMenuViewModel.kt` |
| `NodeCooldown` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/components/NetworkDiagnosticsCard.kt` |
| `BackClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiEvent.kt` |
| `ClearLocalDataClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiEvent.kt` |
| `DeveloperMenuUiEvent` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiEvent.kt` |
| `DisableDeveloperModeClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiEvent.kt` |
| `ErrorLogClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiEvent.kt` |
| `NodesClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiEvent.kt` |
| `DeveloperMenuUiState` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/model/DeveloperMenuUiState.kt` |
| `DeveloperNodesViewModel` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/developer/nodes/DeveloperNodesViewModel.kt` |
| `DisclaimerViewModel` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/DisclaimerViewModel.kt` |
| `DisclaimerType` | enum class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/model/DisclaimerType.kt` |
| `BackClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/model/DisclaimerUiEvent.kt` |
| `DisclaimerUiEvent` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/disclaimer/model/DisclaimerUiEvent.kt` |
| `DeveloperErrorLogViewModel` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/DeveloperErrorLogViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiEvent.kt` |
| `ClearErrorsClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiEvent.kt` |
| `ClearErrorsConfirmed` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiEvent.kt` |
| `ClearErrorsDismissed` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiEvent.kt` |
| `DeveloperErrorLogUiEvent` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiEvent.kt` |
| `DeveloperErrorLogUiState` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorLogUiState.kt` |
| `DeveloperErrorUi` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/errors/model/DeveloperErrorUi.kt` |
| `LicensesViewModel` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/LicensesViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/model/LicensesUiEvent.kt` |
| `LicensesUiEvent` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/model/LicensesUiEvent.kt` |
| `LicensesUiState` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/licenses/model/LicensesUiState.kt` |
| `ControlPlaneActionState` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/ControlPlaneSettingsViewModel.kt` |
| `ControlPlaneFormState` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/ControlPlaneSettingsViewModel.kt` |
| `ControlPlaneSettingsViewModel` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/ControlPlaneSettingsViewModel.kt` |
| `AddClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `AddConfirmed` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `AddDismissed` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `AddSourceChanged` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `BackClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ClearJsonDirectory` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneAddSource` | enum class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneDirectoryError` | enum class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneSettingsError` | enum class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneSettingsUiEvent` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneSettingsUiState` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneUiModel` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneUiSource` | enum class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `ControlPlaneUiStatus` | enum class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `DirectoryApply` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `EditDirectoryClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `NewUrlChanged` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `Refresh` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `Remove` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `RemoveDirectory` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/network/model/ControlPlaneSettingsUiState.kt` |
| `SettingsLocalState` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/SettingsViewModel.kt` |
| `SettingsViewModel` | class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/SettingsViewModel.kt` |
| `SettingsEffect` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsEffect.kt` |
| `ShowSnackbar` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsEffect.kt` |
| `AppLockEnabledChanged` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `AttachmentStorageClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `AutoReplyClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `BlockUnknownContactInvitesChanged` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `BlockedContactsClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `ControlPlanesClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `DataDisclaimerClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `DeveloperMenuClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `DirectIdentitySetupModeChanged` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `LanguagePickerDismissed` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `LanguagePickerOpened` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `LanguageSelected` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `LicensesClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `MessageSafetyEnabledChanged` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `PrivacyPolicyClicked` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `SemanticSearchEnabledChanged` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `SettingsUiEvent` | interface | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `VersionRowTapped` | object | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `VoiceTranscriptionEnabledChanged` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiEvent.kt` |
| `SettingsUiState` | data class | `commonMain` | `feature/settings/src/commonMain/kotlin/com/cbgm/sparrow/feature/settings/presentation/overview/model/SettingsUiState.kt` |
| `SettingsRepositoryLanguageTest` | class | `commonTest` | `feature/settings/src/commonTest/kotlin/com/cbgm/sparrow/feature/settings/data/repository/SettingsRepositoryLanguageTest.kt` |

### `:feature:search`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `MessageSearchIndexDataSource` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/datasource/MessageSearchIndexDataSource.kt` |
| `MessageSearchLocalDataSource` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/datasource/MessageSearchLocalDataSource.kt` |
| `SemanticSearchEmbeddingDataSource` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/datasource/SemanticSearchEmbeddingDataSource.kt` |
| `EmbeddingCodecMapper` | object | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/mapper/EmbeddingCodecMapper.kt` |
| `SemanticSearchIndexConfig` | object | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/model/SemanticSearchIndexConfig.kt` |
| `MessageSearchRepositoryImpl` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/repository/MessageSearchRepositoryImpl.kt` |
| `SemanticSearchRepositoryImpl` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/repository/SemanticSearchRepositoryImpl.kt` |
| `MessageSearchConversationType` | enum class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/MessageSearchConversationType.kt` |
| `MessageSearchMatchType` | enum class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/MessageSearchMatchType.kt` |
| `MessageSearchResult` | data class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/MessageSearchResult.kt` |
| `BuildingIndex` | data class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/SemanticSearchState.kt` |
| `Disabled` | object | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/SemanticSearchState.kt` |
| `DownloadingModel` | data class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/SemanticSearchState.kt` |
| `Failed` | data class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/SemanticSearchState.kt` |
| `Preparing` | object | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/SemanticSearchState.kt` |
| `Ready` | object | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/SemanticSearchState.kt` |
| `SemanticSearchState` | interface | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/SemanticSearchState.kt` |
| `MessageSearchRepository` | interface | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/repository/MessageSearchRepository.kt` |
| `SemanticSearchRepository` | interface | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/repository/SemanticSearchRepository.kt` |
| `InitializeSemanticSearchUseCase` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/InitializeSemanticSearchUseCase.kt` |
| `ObserveSemanticSearchStateUseCase` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/ObserveSemanticSearchStateUseCase.kt` |
| `SearchMessagesUseCase` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/SearchMessagesUseCase.kt` |
| `SetSemanticSearchEnabledUseCase` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/SetSemanticSearchEnabledUseCase.kt` |
| `MessageSearchViewModel` | class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/MessageSearchViewModel.kt` |
| `MessageSearchMode` | enum class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchMode.kt` |
| `MessageSearchResultUi` | data class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchResultUi.kt` |
| `BackClicked` | object | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiEvent.kt` |
| `ClearQueryClicked` | object | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiEvent.kt` |
| `MessageSearchUiEvent` | interface | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiEvent.kt` |
| `QueryChanged` | data class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiEvent.kt` |
| `ResultClicked` | data class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiEvent.kt` |
| `MessageSearchUiState` | data class | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiState.kt` |
| `EmbeddingCodecMapperTest` | class | `commonTest` | `feature/search/src/commonTest/kotlin/com/cbgm/sparrow/feature/search/data/mapper/EmbeddingCodecMapperTest.kt` |
| `MessageSearchEmbeddingTextMapperTest` | class | `commonTest` | `feature/search/src/commonTest/kotlin/com/cbgm/sparrow/feature/search/data/mapper/MessageSearchEmbeddingTextMapperTest.kt` |

### `:feature:safety`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `EmbeddingGemmaParityDeviceTest` | class | `androidDeviceTest` | `feature/safety/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/safety/device/EmbeddingGemmaParityDeviceTest.kt` |
| `Reference` | data class | `androidDeviceTest` | `feature/safety/src/androidDeviceTest/kotlin/com/cbgm/sparrow/feature/safety/device/EmbeddingGemmaParityDeviceTest.kt` |
| `MessageSafetyEmbeddingDataSource` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/datasource/MessageSafetyEmbeddingDataSource.kt` |
| `MessageSafetyLocalDataSource` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/datasource/MessageSafetyLocalDataSource.kt` |
| `GeneratedMessageSafetyMlpModel` | object | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/model/GeneratedMessageSafetyMlpModel.kt` |
| `HeadDto` | data class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/model/GeneratedMessageSafetyMlpModel.kt` |
| `MessageSafetyAnalysisRepositoryImpl` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/repository/MessageSafetyAnalysisRepositoryImpl.kt` |
| `MessageSafetyRepositoryImpl` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/data/repository/MessageSafetyRepositoryImpl.kt` |
| `MessageSafetyAssessment` | data class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyAssessment.kt` |
| `MessageSafetyCandidate` | data class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyCandidate.kt` |
| `MessageSafetyReason` | enum class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyReason.kt` |
| `Analyzing` | object | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyState.kt` |
| `Disabled` | object | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyState.kt` |
| `Failed` | data class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyState.kt` |
| `MessageSafetyState` | interface | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyState.kt` |
| `Preparing` | object | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyState.kt` |
| `Ready` | object | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/model/MessageSafetyState.kt` |
| `MessageSafetyAnalysisRepository` | interface | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/repository/MessageSafetyAnalysisRepository.kt` |
| `MessageSafetyRepository` | interface | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/repository/MessageSafetyRepository.kt` |
| `AnalyzeMessageSafetyUseCase` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/AnalyzeMessageSafetyUseCase.kt` |
| `InitializeMessageSafetyUseCase` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/InitializeMessageSafetyUseCase.kt` |
| `SafetyRuntimeState` | data class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/InitializeMessageSafetyUseCase.kt` |
| `ObserveMessageSafetyAssessmentsUseCase` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/ObserveMessageSafetyAssessmentsUseCase.kt` |
| `ObserveMessageSafetyStateUseCase` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/ObserveMessageSafetyStateUseCase.kt` |
| `ProcessMessageSafetyBatchUseCase` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/ProcessMessageSafetyBatchUseCase.kt` |
| `BlockActionState` | data class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/MessageSafetyDetailsViewModel.kt` |
| `MessageSafetyDetailsViewModel` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/MessageSafetyDetailsViewModel.kt` |
| `BackClicked` | object | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyDetailsUiEvent.kt` |
| `BlockUserClicked` | object | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyDetailsUiEvent.kt` |
| `MessageSafetyDetailsUiEvent` | interface | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyDetailsUiEvent.kt` |
| `MessageSafetyDetailsUiState` | data class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyDetailsUiState.kt` |
| `MessageSafetyWarningLevel` | enum class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyWarningLevel.kt` |
| `MessageSafetyWarningReason` | enum class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyWarningReason.kt` |
| `MessageSafetyWarningUi` | data class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/model/MessageSafetyWarningUi.kt` |
| `MessageSafetyClassifier` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/util/MessageSafetyClassifier.kt` |
| `MessageSafetyStructuralAnalyzer` | class | `commonMain` | `feature/safety/src/commonMain/kotlin/com/cbgm/sparrow/feature/safety/util/MessageSafetyStructuralAnalyzer.kt` |
| `MessageSafetyEmbeddingDataSourceTest` | class | `commonTest` | `feature/safety/src/commonTest/kotlin/com/cbgm/sparrow/feature/safety/data/datasource/MessageSafetyEmbeddingDataSourceTest.kt` |
| `RecordingEmbedder` | class | `commonTest` | `feature/safety/src/commonTest/kotlin/com/cbgm/sparrow/feature/safety/data/datasource/MessageSafetyEmbeddingDataSourceTest.kt` |
| `AnalyzeMessageSafetyUseCaseTest` | class | `commonTest` | `feature/safety/src/commonTest/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/AnalyzeMessageSafetyUseCaseTest.kt` |
| `FakeAnalysisRepository` | class | `commonTest` | `feature/safety/src/commonTest/kotlin/com/cbgm/sparrow/feature/safety/domain/usecase/AnalyzeMessageSafetyUseCaseTest.kt` |
| `MessageSafetyUiMapperTest` | class | `commonTest` | `feature/safety/src/commonTest/kotlin/com/cbgm/sparrow/feature/safety/presentation/details/mapper/MessageSafetyUiMapperTest.kt` |
| `MessageSafetyClassifierTest` | class | `commonTest` | `feature/safety/src/commonTest/kotlin/com/cbgm/sparrow/feature/safety/util/MessageSafetyClassifierTest.kt` |
| `MessageSafetyStructuralAnalyzerTest` | class | `commonTest` | `feature/safety/src/commonTest/kotlin/com/cbgm/sparrow/feature/safety/util/MessageSafetyStructuralAnalyzerTest.kt` |

### `:quality:detekt-rules`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `SparrowRuleSetProvider` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/SparrowRuleSetProvider.kt` |
| `DaoUsageRule` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/DaoUsageRule.kt` |
| `LayerDependencyRule` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/LayerDependencyRule.kt` |
| `NoNotNullAssertionRule` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/NoNotNullAssertionRule.kt` |
| `NoPlatformImportInCommonMainRule` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/NoPlatformImportInCommonMainRule.kt` |
| `NoTestImportInProductionRule` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/NoTestImportInProductionRule.kt` |
| `RepositoryDependencyRule` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/RepositoryDependencyRule.kt` |
| `UseCaseDependencyRule` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/UseCaseDependencyRule.kt` |
| `ViewModelDirectDataDependencyRule` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/ViewModelDirectDataDependencyRule.kt` |
| `WeakHashAlgorithmRule` | class | `main` | `quality/detekt-rules/src/main/kotlin/com/cbgm/sparrow/detekt/rule/WeakHashAlgorithmRule.kt` |

### `:server:protocol`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `BlobMetadata` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/BlobModels.kt` |
| `BlobUploadTicketClaims` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/BlobModels.kt` |
| `CreateMailboxRequest` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `CreateMailboxResponse` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `DeliveryRoute` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `EnvelopeAcceptanceState` | enum class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `ErrorResponse` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `FederatedEnvelope` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `FederatedIndicatorEvent` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `FederationAcknowledgement` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `MailboxEnvelopeRequest` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `MailboxEnvelopesResponse` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/EnvelopeModels.kt` |
| `AcknowledgeEnvelope` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `BlobUploadTicketIssued` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `BlobUploadTicketRejected` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `EnvelopeAccepted` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `Error` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `GatewayClientMessage` | interface | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `GatewayServerMessage` | interface | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `IncomingEnvelope` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `IndicatorState` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `IndicatorState` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `PendingTransportEnvelopesResponse` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `PushDeviceRegistrationRequest` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `RefreshRoute` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `Register` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `Registered` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `RequestBlobUploadTicket` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `RouteRegistered` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `SendEnvelope` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `SendFederatedEnvelope` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `TransportEnvelope` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/GatewayModels.kt` |
| `NodeCapability` | enum class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `NodeDirectory` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `NodeHeartbeatRequest` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `NodeRegistrationRequest` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `RegistryAuthorityCertificate` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `RegistrySigningCertificate` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `SignedNodeDirectory` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `SparrowNodeDescriptor` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `UnsignedNodeDescriptor` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `UnsignedNodeHeartbeat` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `UnsignedRegistryAuthorityCertificate` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `UnsignedRegistrySigningCertificate` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/NodeModels.kt` |
| `ClientRoute` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `ClientRouteRegistration` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `ClientRoutingResult` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `GatewayLoad` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `GatewayNodeInformation` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `UnsignedClientRoute` | data class | `main` | `server/protocol/src/main/kotlin/com/cbgm/sparrow/server/protocol/PresenceModels.kt` |
| `GatewaySerializationTest` | class | `test` | `server/protocol/src/test/kotlin/com/cbgm/sparrow/server/protocol/GatewaySerializationTest.kt` |

### `:server:security`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ClientRoutingIds` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/ClientRoutingIds.kt` |
| `CommandLineOutput` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/CommandLineOutput.kt` |
| `InternalApiAuthentication` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/InternalApiAuthentication.kt` |
| `NodeIdentity` | data class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeIdentity.kt` |
| `NodeIdentityStore` | class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeIdentityStore.kt` |
| `NodeIds` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeIds.kt` |
| `NodeRequestAuthentication` | data class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeRequestHeaders` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeRequestSigner` | class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeRequestVerifier` | class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeDescriptorResolver` | interface | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthorizer.kt` |
| `NodeRequestAuthorizationRequirements` | data class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthorizer.kt` |
| `NodeRequestAuthorizer` | class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthorizer.kt` |
| `NodeRequestSignatureCli` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestSignatureCli.kt` |
| `PresenceRouteRegistrationCli` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/PresenceRouteRegistrationCli.kt` |
| `ProtocolSignatures` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/ProtocolSignatures.kt` |
| `RegistryCertificateSignatures` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RegistryCertificateSignatures.kt` |
| `ReplayProtection` | class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/ReplayProtection.kt` |
| `Allowed` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `BoundedRateLimiter` | class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `ClientRateLimitKeys` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `RateLimitDecision` | interface | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `RateLimitPolicy` | data class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `Rejected` | data class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `Window` | data class | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `Signatures` | object | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/Signatures.kt` |
| `InternalApiAuthenticationTest` | class | `test` | `server/security/src/test/kotlin/com/cbgm/sparrow/server/security/InternalApiAuthenticationTest.kt` |
| `AuthorizationFixture` | data class | `test` | `server/security/src/test/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthorizerTest.kt` |
| `NodeRequestAuthorizerTest` | class | `test` | `server/security/src/test/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthorizerTest.kt` |
| `ProtocolSignaturesTest` | class | `test` | `server/security/src/test/kotlin/com/cbgm/sparrow/server/security/ProtocolSignaturesTest.kt` |
| `RequestRateLimitingTest` | class | `test` | `server/security/src/test/kotlin/com/cbgm/sparrow/server/security/RequestRateLimitingTest.kt` |

### `:server:persistence`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `BoundedIdempotencyStore` | class | `main` | `server/persistence/src/main/kotlin/com/cbgm/sparrow/server/persistence/BoundedIdempotencyStore.kt` |
| `ControlPlaneEndpointPool` | class | `main` | `server/persistence/src/main/kotlin/com/cbgm/sparrow/server/persistence/ServiceEnvironment.kt` |
| `ServiceEnvironment` | object | `main` | `server/persistence/src/main/kotlin/com/cbgm/sparrow/server/persistence/ServiceEnvironment.kt` |
| `ControlPlaneEndpointPoolTest` | class | `test` | `server/persistence/src/test/kotlin/com/cbgm/sparrow/server/persistence/ServiceEnvironmentTest.kt` |
| `ServiceEnvironmentTest` | class | `test` | `server/persistence/src/test/kotlin/com/cbgm/sparrow/server/persistence/ServiceEnvironmentTest.kt` |

### `:server:observability`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ReadinessProbe` | object | `main` | `server/observability/src/main/kotlin/com/cbgm/sparrow/server/observability/ReadinessProbe.kt` |
| `ServerObservabilityTest` | class | `test` | `server/observability/src/test/kotlin/com/cbgm/sparrow/server/observability/ServerObservabilityTest.kt` |

### `:server:node-registry`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `NodeRegistryConfig` | data class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/Application.kt` |
| `Accepted` | object | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `NodeRegistryStorage` | interface | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `NodeRegistryStore` | class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `RegisteredNode` | data class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `RegistrationResult` | interface | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `Rejected` | data class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `PostgresNodeRegistryDatabase` | class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryDatabase.kt` |
| `PostgresNodeRegistryDatabaseConfig` | data class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryDatabase.kt` |
| `PostgresNodeRegistryStore` | class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryStore.kt` |
| `RegistryAuthorityCertificateStore` | class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryAuthorityCertificateStore.kt` |
| `RegistryAuthorityProvisioningCli` | object | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryAuthorityProvisioningCli.kt` |
| `CertifiedRegistrySigner` | data class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `DirectRegistryDirectorySigner` | class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `RegistryDirectorySigner` | interface | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `RegistrySigningConfig` | data class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `RotatingRegistryDirectorySigner` | class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `RegistrySigningRuntime` | data class | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistrySigningRuntime.kt` |
| `NodeRegistryApplicationRateLimitTest` | class | `test` | `server/node-registry/src/test/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryApplicationRateLimitTest.kt` |
| `NodeRegistryStoreTest` | class | `test` | `server/node-registry/src/test/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStoreTest.kt` |
| `PostgresNodeRegistryStoreIntegrationTest` | class | `test` | `server/node-registry/src/test/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryStoreIntegrationTest.kt` |
| `RegistryDirectorySignerTest` | class | `test` | `server/node-registry/src/test/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySignerTest.kt` |

### `:server:presence-directory`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `PresenceConfig` | data class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/Application.kt` |
| `CachingPhoneTokenVerifier` | class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/CachingPhoneTokenVerifier.kt` |
| `FirebasePhoneTokenVerifier` | class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/FirebasePhoneTokenVerifier.kt` |
| `PhoneTokenVerifier` | interface | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PhoneVerification.kt` |
| `PhoneVerification` | class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PhoneVerification.kt` |
| `VerifiedPhone` | data class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PhoneVerification.kt` |
| `PresenceRouteKey` | data class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceRoutes.kt` |
| `PresenceRuntime` | data class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceRuntime.kt` |
| `Accepted` | object | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceStore.kt` |
| `PresenceResult` | interface | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceStore.kt` |
| `PresenceStorage` | interface | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceStore.kt` |
| `PresenceStore` | class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceStore.kt` |
| `Rejected` | data class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/PresenceStore.kt` |
| `RedisPresenceStore` | class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/RedisPresenceStore.kt` |
| `RoutingKeys` | data class | `main` | `server/presence-directory/src/main/kotlin/com/cbgm/sparrow/server/presence/RedisPresenceStore.kt` |
| `CachingPhoneTokenVerifierTest` | class | `test` | `server/presence-directory/src/test/kotlin/com/cbgm/sparrow/server/presence/CachingPhoneTokenVerifierTest.kt` |
| `PhoneVerificationTest` | class | `test` | `server/presence-directory/src/test/kotlin/com/cbgm/sparrow/server/presence/PhoneVerificationTest.kt` |
| `PresenceStoreTest` | class | `test` | `server/presence-directory/src/test/kotlin/com/cbgm/sparrow/server/presence/PresenceStoreTest.kt` |
| `RedisPresenceStoreIntegrationTest` | class | `test` | `server/presence-directory/src/test/kotlin/com/cbgm/sparrow/server/presence/RedisPresenceStoreIntegrationTest.kt` |

### `:server:gateway`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `GatewayConfig` | data class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/Application.kt` |
| `BlobCleanupAgent` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobCleanupAgent.kt` |
| `BlobAlreadyExistsException` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobPaths` | data class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobStorageCapacityExceededException` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobStore` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobTooLargeException` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobUploadPermitCleanupAgent` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobUploadPermitCleanupAgent.kt` |
| `BlobUploadPermitStore` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobUploadPermitStore.kt` |
| `ConnectionRegistry` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/ConnectionRegistry.kt` |
| `GatewayConnection` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/ConnectionRegistry.kt` |
| `BoundedPresenceClient` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayBackgroundClients.kt` |
| `GatewayPushDispatcher` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayBackgroundClients.kt` |
| `GatewayBlobUploadTicketIssuer` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayBlobUploadTicketIssuer.kt` |
| `GatewayControlPlaneDirectory` | data class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayControlPlaneDiscovery.kt` |
| `GatewayControlPlaneDiscovery` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayControlPlaneDiscovery.kt` |
| `FederationClient` | interface | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |
| `LegacyPushClient` | interface | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |
| `PresenceClient` | interface | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |
| `PresenceRegistrationResult` | enum class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |
| `GatewayRouteValidationFailure` | enum class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayRouteValidator.kt` |
| `GatewayRouteValidator` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayRouteValidator.kt` |
| `GatewayRuntime` | data class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayRuntime.kt` |
| `GatewayMessageActions` | data class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewayPushActions` | data class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewaySessionHandler` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewaySessionState` | data class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewaySessionWorkDispatcher` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionWorkDispatcher.kt` |
| `EnvelopeFallbackActions` | data class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayWebSocketHandler.kt` |
| `GatewayWebSocketHandler` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayWebSocketHandler.kt` |
| `HttpFederationClient` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `HttpLegacyPushClient` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `HttpNodePushClient` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `HttpPresenceClient` | class | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `GatewayBackgroundClientsTest` | class | `test` | `server/gateway/src/test/kotlin/com/cbgm/sparrow/server/gateway/GatewayBackgroundClientsTest.kt` |
| `GatewayBlobUploadTicketIssuerTest` | class | `test` | `server/gateway/src/test/kotlin/com/cbgm/sparrow/server/gateway/GatewayBlobUploadTicketIssuerTest.kt` |
| `GatewayEnvelopeRoutingTest` | class | `test` | `server/gateway/src/test/kotlin/com/cbgm/sparrow/server/gateway/GatewayEnvelopeRoutingTest.kt` |
| `GatewayRouteValidatorTest` | class | `test` | `server/gateway/src/test/kotlin/com/cbgm/sparrow/server/gateway/GatewayRouteValidatorTest.kt` |
| `GatewaySessionWorkDispatcherTest` | class | `test` | `server/gateway/src/test/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionWorkDispatcherTest.kt` |

### `:server:federation`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `FederationConfig` | data class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/Application.kt` |
| `CachingNodeRegistryClient` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/CachingNodeRegistryClient.kt` |
| `FederationPeerRouter` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPeerRouter.kt` |
| `LocalGatewayClient` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `LocalIndicatorGatewayClient` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `LocalRouteResolver` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `MailboxClient` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `NodeRegistryClient` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `PeerNodeDirectory` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `PresenceDirectoryClient` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `RemoteFederationClient` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `RemoteIndicatorFederationClient` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `RemoteRouteResolver` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPorts.kt` |
| `FederationRouter` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationRouter.kt` |
| `FederationRuntime` | data class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationRuntime.kt` |
| `ManagedHttpClient` | data class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationRuntime.kt` |
| `HttpLocalGatewayClient` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `HttpMailboxClient` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `HttpPresenceDirectoryClient` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `HttpRemoteFederationClient` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `GatewayLoadProvider` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `HttpGatewayLoadProvider` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `NodeRegistrationAgent` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `NodeRegistrationClient` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `NodeRegistrationConfig` | data class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `OutboundEnvelopeEntry` | data class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeQueue.kt` |
| `OutboundEnvelopeQueue` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeQueue.kt` |
| `OutboundEnvelopeStorage` | interface | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeQueue.kt` |
| `OutboundEnvelopeRetryAgent` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeRetryAgent.kt` |
| `PostgresOutboundEnvelopeDatabase` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeDatabase.kt` |
| `PostgresOutboundEnvelopeDatabaseConfig` | data class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeDatabase.kt` |
| `PostgresOutboundEnvelopeStorage` | class | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeStorage.kt` |
| `FederationRouterTest` | class | `test` | `server/federation/src/test/kotlin/com/cbgm/sparrow/server/federation/FederationRouterTest.kt` |
| `TestPeerFederationClient` | class | `test` | `server/federation/src/test/kotlin/com/cbgm/sparrow/server/federation/FederationRouterTest.kt` |
| `TestPeerNodeRegistry` | class | `test` | `server/federation/src/test/kotlin/com/cbgm/sparrow/server/federation/FederationRouterTest.kt` |
| `NodeRegistrationConfigTest` | class | `test` | `server/federation/src/test/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationConfigTest.kt` |
| `PostgresOutboundEnvelopeStorageIntegrationTest` | class | `test` | `server/federation/src/test/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeStorageIntegrationTest.kt` |

### `:server:mailbox`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `MailboxConfig` | data class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/Application.kt` |
| `MailboxPushNotifier` | class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxPushNotifier.kt` |
| `Created` | data class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `GlobalQuotaExceeded` | object | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `InMemoryMailbox` | data class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxCreationResult` | interface | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxResult` | interface | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxRevocationResult` | interface | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxStorage` | interface | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxStore` | class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `NotFound` | object | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `OwnerQuotaExceeded` | object | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `Rejected` | data class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `Revoked` | object | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `Stored` | data class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `Unauthorized` | object | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `PostgresMailboxDatabase` | class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxDatabase.kt` |
| `PostgresMailboxDatabaseConfig` | data class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxDatabase.kt` |
| `MailboxAuthorization` | data class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxQueries.kt` |
| `PostgresMailboxQueries` | class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxQueries.kt` |
| `EnvelopeInsertParameter` | object | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxStore.kt` |
| `MailboxCredentials` | data class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxStore.kt` |
| `MailboxInsertParameter` | object | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxStore.kt` |
| `PostgresMailboxStore` | class | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxStore.kt` |
| `MailboxApplicationRateLimitTest` | class | `test` | `server/mailbox/src/test/kotlin/com/cbgm/sparrow/server/mailbox/MailboxApplicationRateLimitTest.kt` |
| `MailboxStoreTest` | class | `test` | `server/mailbox/src/test/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStoreTest.kt` |
| `PostgresMailboxStoreIntegrationTest` | class | `test` | `server/mailbox/src/test/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxStoreIntegrationTest.kt` |

### `:server:push`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `PushConfig` | data class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/Application.kt` |
| `FirebasePushSender` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/FirebasePushSender.kt` |
| `PostgresPendingEnvelopeStore` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPendingEnvelopeStore.kt` |
| `PostgresPushDatabase` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPushDatabase.kt` |
| `PostgresPushDatabaseConfig` | data class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPushDatabase.kt` |
| `PostgresPushDeviceStore` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPushDeviceStore.kt` |
| `PostgresWakeUpStore` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresWakeUpStore.kt` |
| `PushCoordinator` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushCoordinator.kt` |
| `NodeRouteOwnershipResolver` | interface | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushRuntime.kt` |
| `PushNodeApiRuntime` | data class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushRuntime.kt` |
| `PushRuntime` | data class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushRuntime.kt` |
| `Entry` | data class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `InMemoryPendingEnvelopeStore` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `InMemoryPushDeviceStore` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `InMemoryWakeUpStore` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PendingEnvelopeStore` | interface | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PushDevice` | data class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PushDeviceStore` | interface | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PushStores` | class | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `WakeUpStore` | interface | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `InMemoryPushStoresTest` | class | `test` | `server/push/src/test/kotlin/com/cbgm/sparrow/server/push/InMemoryPushStoresTest.kt` |
| `PendingEnvelopeStoreTest` | class | `test` | `server/push/src/test/kotlin/com/cbgm/sparrow/server/push/PendingEnvelopeStoreTest.kt` |
| `PostgresPushStoresIntegrationTest` | class | `test` | `server/push/src/test/kotlin/com/cbgm/sparrow/server/push/PostgresPushStoresIntegrationTest.kt` |
| `PushCoordinatorTest` | class | `test` | `server/push/src/test/kotlin/com/cbgm/sparrow/server/push/PushCoordinatorTest.kt` |

### `:server:link-preview`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `LinkPreviewFetcher` | class | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewFetcher.kt` |
| `YouTubeMetadata` | data class | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewFetcher.kt` |
| `FetchedLinkPreview` | data class | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewModels.kt` |
| `LinkPreviewImage` | data class | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewModels.kt` |
| `LinkPreviewRequest` | data class | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewModels.kt` |
| `LinkPreviewResponse` | data class | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewModels.kt` |
| `CachedImageSource` | data class | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewService.kt` |
| `CachedPreview` | data class | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewService.kt` |
| `LinkPreviewService` | class | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewService.kt` |
| `LinkPreviewUrlValidator` | object | `main` | `server/link-preview/src/main/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewUrlValidator.kt` |
| `LinkPreviewHtmlParserTest` | class | `test` | `server/link-preview/src/test/kotlin/com/cbgm/sparrow/server/linkpreview/LinkPreviewHtmlParserTest.kt` |

### `:server:exchange-rates`

| Declaration | Kind | Source set | Path |
|---|---|---|---|
| `ExchangeRateCache` | class | `main` | `server/exchange-rates/src/main/kotlin/com/cbgm/sparrow/server/exchangerates/ExchangeRateCache.kt` |
| `ExchangeRateCacheTest` | class | `test` | `server/exchange-rates/src/test/kotlin/com/cbgm/sparrow/server/exchangerates/ExchangeRateCacheTest.kt` |
