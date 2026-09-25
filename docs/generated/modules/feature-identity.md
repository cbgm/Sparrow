# `:feature:identity`

Source directory: `feature/identity`

## Direct project dependencies

- `:core`
- `:data:datastore`
- `:data:database`
- `:core:crypto`
- `:core:protocol`
- `:core:ui`
- `:feature:avatar`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidIdentityBackupCodec` | `class` | `androidMain` | `feature/identity/src/androidMain/kotlin/com/cbgm/sparrow/feature/identity/device/AndroidIdentityBackupCodec.kt` |
| `AndroidPrivateKeyStorage` | `class` | `androidMain` | `feature/identity/src/androidMain/kotlin/com/cbgm/sparrow/feature/identity/device/AndroidPrivateKeyStorage.kt` |
| `IdentityLocalResetHandler` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/IdentityLocalResetHandler.kt` |
| `ApprovedIdentityReconnectionDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/ApprovedIdentityReconnectionDataSource.kt` |
| `IdentityBackupStatusDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityBackupStatusDataSource.kt` |
| `IdentityExchangeDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityExchangeDataSource.kt` |
| `IdentityExchangeStoreDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityExchangeStoreDataSource.kt` |
| `IdentityVerificationDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/IdentityVerificationDataSource.kt` |
| `LocalIdentityProfileDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/LocalIdentityProfileDataSource.kt` |
| `LocalIdentitySharingDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/LocalIdentitySharingDataSource.kt` |
| `LocalProfilePictureDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/LocalProfilePictureDataSource.kt` |
| `ManualIdentityExchangeDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/ManualIdentityExchangeDataSource.kt` |
| `PendingRemoteIdentityChangeDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/PendingRemoteIdentityChangeDataSource.kt` |
| `ProfilePictureFileDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/ProfilePictureFileDataSource.kt` |
| `PublicIdentityDataSource` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/PublicIdentityDataSource.kt` |
| `RemoteIdentityDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/RemoteIdentityDataSource.kt` |
| `RemoteProfilePictureDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/RemoteProfilePictureDataSource.kt` |
| `SparrowDataStorePublicIdentityDataSource` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/datasource/SparrowDataStorePublicIdentityDataSource.kt` |
| `IdentityAcceptanceReviewRequiredDtoException` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/IdentityAcceptanceReviewRequiredDtoException.kt` |
| `IdentityExchangeBindingDto` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/IdentityExchangeBindingDto.kt` |
| `IdentityExchangeStage` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/IdentityExchangeStage.kt` |
| `RemoteIdentityImportOriginDto` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/RemoteIdentityImportOriginDto.kt` |
| `StoredKeyExchangeStatusDto` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/StoredRemoteIdentityStateDto.kt` |
| `StoredVerificationStatusDto` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/model/StoredRemoteIdentityStateDto.kt` |
| `IdentityVerificationReceiptEncoder` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/protocol/IdentityVerificationReceiptEncoder.kt` |
| `ApprovedIdentityReconnectionRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/ApprovedIdentityReconnectionRepositoryImpl.kt` |
| `IdentityBackupRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityBackupRepositoryImpl.kt` |
| `IdentityExchangeRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityExchangeRepositoryImpl.kt` |
| `IdentityRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityRepositoryImpl.kt` |
| `IdentityShareRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityShareRepositoryImpl.kt` |
| `IdentityVerificationRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/IdentityVerificationRepositoryImpl.kt` |
| `LocalIdentityProfileRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/LocalIdentityProfileRepositoryImpl.kt` |
| `LocalIdentitySharingRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/LocalIdentitySharingRepositoryImpl.kt` |
| `LocalProfilePictureRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/LocalProfilePictureRepositoryImpl.kt` |
| `PendingRemoteIdentityChangeRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/PendingRemoteIdentityChangeRepositoryImpl.kt` |
| `RemoteIdentityImportRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteIdentityImportRepositoryImpl.kt` |
| `RemoteIdentityReadRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteIdentityReadRepositoryImpl.kt` |
| `RemoteProfilePictureRepositoryImpl` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/data/repository/RemoteProfilePictureRepositoryImpl.kt` |
| `IdentityBackupCodec` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/IdentityBackupCodec.kt` |
| `IdentityExportRequest` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/IdentityBackupDocumentLauncher.kt` |
| `PhoneNumberHintResult` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PhoneNumberHintLauncher.kt` |
| `PrivateKeyStorage` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/device/PrivateKeyStorage.kt` |
| `IdentityAcceptanceRequiresReviewException` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/error/IdentityAcceptanceRequiresReviewException.kt` |
| `ApprovedIdentityReconnection` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/ApprovedIdentityReconnection.kt` |
| `ContactVerificationStatus` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/ContactVerificationStatus.kt` |
| `DirectIdentitySetupMode` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/DirectIdentitySetupMode.kt` |
| `IdentityBackup` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityBackup.kt` |
| `IdentityBackupStatus` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityBackupStatus.kt` |
| `IdentityExchange` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchange.kt` |
| `IdentityExchangeAcceptance` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInput.kt` |
| `IdentityExchangeBinding` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchangeBinding.kt` |
| `IdentityExchangeClosure` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchangeClosure.kt` |
| `IdentityExchangeClosurePhase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchangeClosure.kt` |
| `IdentityExchangeDirection` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityExchange.kt` |
| `IdentityExchangeOffer` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInput.kt` |
| `IdentityExchangeReady` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeInput.kt` |
| `IdentityHandshakeState` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityHandshakeState.kt` |
| `IdentityPeerState` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityPeerState.kt` |
| `IdentityResult` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityResult.kt` |
| `IdentityResultStatus` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityResult.kt` |
| `IdentityStatus` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/IdentityStatus.kt` |
| `KeyExchangeStatus` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/KeyExchangeStatus.kt` |
| `LocalProfilePicture` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/LocalProfilePicture.kt` |
| `PendingRemoteIdentityChange` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PendingRemoteIdentityChange.kt` |
| `PublicIdentity` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/PublicIdentity.kt` |
| `RemoteIdentityOrigin` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemoteIdentityOrigin.kt` |
| `RemoteIdentityUpdate` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemoteIdentityUpdate.kt` |
| `RemotePeerIdentity` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemotePeerIdentity.kt` |
| `RemoteProfilePicture` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/RemoteProfilePicture.kt` |
| `SharedContactDetails` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/SharedIdentityPayload.kt` |
| `SharedIdentityPayload` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/model/SharedIdentityPayload.kt` |
| `ApprovedIdentityReconnectionRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/ApprovedIdentityReconnectionRepository.kt` |
| `DirectIdentitySetupModeRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/DirectIdentitySetupModeRepository.kt` |
| `IdentityBackupRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityBackupRepository.kt` |
| `IdentityExchangeRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityExchangeRepository.kt` |
| `IdentityRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityRepository.kt` |
| `IdentityShareRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityShareRepository.kt` |
| `IdentityVerificationRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/IdentityVerificationRepository.kt` |
| `LocalIdentityProfileRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/LocalIdentityProfileRepository.kt` |
| `LocalIdentitySharingRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/LocalIdentitySharingRepository.kt` |
| `LocalProfilePictureRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/LocalProfilePictureRepository.kt` |
| `PendingRemoteIdentityChangeRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/PendingRemoteIdentityChangeRepository.kt` |
| `RemoteIdentityImportRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/RemoteIdentityImportRepository.kt` |
| `RemoteIdentityReadRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/RemoteIdentityReadRepository.kt` |
| `RemoteProfilePictureRepository` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/repository/RemoteProfilePictureRepository.kt` |
| `AcceptIdentityExchangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/AcceptIdentityExchangeUseCase.kt` |
| `AcceptRemoteIdentityHandshakeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/AcceptRemoteIdentityHandshakeUseCase.kt` |
| `AcknowledgeQueuedRecoveryInvitationUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/AcknowledgeQueuedRecoveryInvitationUseCase.kt` |
| `ApplyRemoteProfilePictureMetadataUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ApplyRemoteProfilePictureMetadataUseCase.kt` |
| `ApprovePendingRemoteIdentityChangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ApprovePendingRemoteIdentityChangeUseCase.kt` |
| `CancelIdentityExchangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CancelIdentityExchangeUseCase.kt` |
| `CloseIdentityExchangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CloseIdentityExchangeUseCase.kt` |
| `CreateIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CreateIdentityUseCase.kt` |
| `CreateSharedIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/CreateSharedIdentityUseCase.kt` |
| `DeclineIdentityExchangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DeclineIdentityExchangeUseCase.kt` |
| `DeclinePendingRemoteIdentityChangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DeclinePendingRemoteIdentityChangeUseCase.kt` |
| `DecodeSharedIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DecodeSharedIdentityUseCase.kt` |
| `DismissPendingRemoteIdentityChangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/DismissPendingRemoteIdentityChangeUseCase.kt` |
| `EnsureRemoteSigningIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/EnsureRemoteSigningIdentityUseCase.kt` |
| `EstablishMutualIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/EstablishMutualIdentityUseCase.kt` |
| `FindRemoteIdentityPeerIdUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/FindRemoteIdentityPeerIdUseCase.kt` |
| `GetApprovedIdentityReconnectionUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetApprovedIdentityReconnectionUseCase.kt` |
| `GetIdentityBackupStatusUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `GetIdentityExchangeBindingUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityExchangeBindingUseCase.kt` |
| `GetIdentityExchangeClosureUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityExchangeClosureUseCase.kt` |
| `GetIdentityPeerStateUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityPeerStateUseCase.kt` |
| `GetIdentityStatusUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetIdentityStatusUseCase.kt` |
| `GetLocalPhoneNumberUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetLocalPhoneNumberUseCase.kt` |
| `GetPublicIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetPublicIdentityUseCase.kt` |
| `GetRemoteIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/GetRemoteIdentityUseCase.kt` |
| `HandleIdentityVerificationReceiptUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/HandleIdentityVerificationReceiptUseCase.kt` |
| `ImportRemoteIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ImportRemoteIdentityUseCase.kt` |
| `InvalidateIdentityExchangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/InvalidateIdentityExchangeUseCase.kt` |
| `MarkIdentityBackupExportedUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `NormalizeLocalPhoneNumberUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/NormalizeLocalPhoneNumberUseCase.kt` |
| `ObserveApprovedIdentityReconnectionsUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveApprovedIdentityReconnectionsUseCase.kt` |
| `ObserveIdentityHandshakeStateUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveIdentityHandshakeStateUseCase.kt` |
| `ObserveIdentityResultsUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveIdentityResultsUseCase.kt` |
| `ObserveLocalIdentityReadyUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveLocalIdentityReadyUseCase.kt` |
| `ObserveLocalIdentitySharedUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveLocalIdentitySharedUseCase.kt` |
| `ObserveLocalProfilePictureUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveLocalProfilePictureUseCase.kt` |
| `ObservePendingRemoteIdentityChangesUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObservePendingRemoteIdentityChangesUseCase.kt` |
| `ObserveRemoteIdentitiesUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ObserveRemoteIdentitiesUseCase.kt` |
| `PrepareIdentityBackupUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `ReassignIdentityExchangePeerUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReassignIdentityExchangePeerUseCase.kt` |
| `ReceiveIdentityAcknowledgementUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityAcknowledgementUseCase.kt` |
| `ReceiveIdentityExchangeAcceptedUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityExchangeAcceptedUseCase.kt` |
| `ReceiveIdentityExchangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityExchangeUseCase.kt` |
| `ReceiveIdentityReadyUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveIdentityReadyUseCase.kt` |
| `ReceiveManualIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/ReceiveManualIdentityUseCase.kt` |
| `RecordLocalIdentitySharedUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecordLocalIdentitySharedUseCase.kt` |
| `RecordRemoteIdentityDeclineUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecordRemoteIdentityDeclineUseCase.kt` |
| `RecoverIncompleteIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecoverIncompleteIdentityUseCase.kt` |
| `RecoverManualIdentityExchangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RecoverManualIdentityExchangeUseCase.kt` |
| `RemoveLocalProfilePictureUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/RemoveLocalProfilePictureUseCase.kt` |
| `RestoreIdentityBackupUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/IdentityBackupUseCases.kt` |
| `SaveLocalPhoneNameUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SaveLocalPhoneNameUseCase.kt` |
| `SendIdentityVerificationReceiptUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SendIdentityVerificationReceiptUseCase.kt` |
| `SetLocalProfilePictureUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/SetLocalProfilePictureUseCase.kt` |
| `StagePendingRemoteIdentityChangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StagePendingRemoteIdentityChangeUseCase.kt` |
| `StageRemoteIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StageRemoteIdentityUseCase.kt` |
| `StartIdentityExchangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StartIdentityExchangeUseCase.kt` |
| `StartManualIdentityExchangeUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/StartManualIdentityExchangeUseCase.kt` |
| `VerifyRemoteIdentityUseCase` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/domain/usecase/VerifyRemoteIdentityUseCase.kt` |
| `IdentityViewModel` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/IdentityViewModel.kt` |
| `MeDetailPage` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/IdentityScreen.kt` |
| `IdentityBackupUiState` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/model/IdentityBackupUiState.kt` |
| `IdentityBackupUiStatus` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/model/IdentityBackupUiState.kt` |
| `IdentityUiEvent` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/model/IdentityUiEvent.kt` |
| `IdentityUiState` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/model/IdentityUiState.kt` |
| `IdentityProfilePictureUiState` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/profile/IdentityProfilePictureUiState.kt` |
| `IdentityProfilePictureViewModel` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/setup/profile/IdentityProfilePictureViewModel.kt` |
| `ShareIdentityViewModel` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/ShareIdentityViewModel.kt` |
| `ShareIdentityUiEvent` | `interface` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/model/ShareIdentityUiEvent.kt` |
| `ShareIdentityUiState` | `class` | `commonMain` | `feature/identity/src/commonMain/kotlin/com/cbgm/sparrow/feature/identity/presentation/share/model/ShareIdentityUiState.kt` |
