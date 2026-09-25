# Identity

`:feature:identity` owns local cryptographic identity, identity exchange/trust state, encrypted identity backup/restore, remote identity-change review and approved reconnection persistence.

## Core identity

Key types include:

- `IdentityRepository` / `IdentityRepositoryImpl`
- `PublicIdentity`
- `CreateIdentityUseCase`
- `IdentityLocalEncryptionKeyPairProvider`
- `IdentityLocalSigningKeyPairProvider`
- `SodiumIdentityKeyGenerator` in `:core:crypto`
- `PrivateKeyStorage` / Android private-key storage
- `LocalIdentityProfileDataSource`
- `GetLocalPhoneNumberUseCase`, `NormalizeLocalPhoneNumberUseCase`, `SaveLocalPhoneNameUseCase`

The public identity contains the public encryption/signing keys plus public profile metadata needed by the protocol. Private keys stay local except when the user explicitly exports an encrypted identity backup.

## Identity exchange

The persisted exchange state machine is implemented by `IdentityExchangeDataSource` and exposed through focused use cases including:

- `StartIdentityExchangeUseCase`
- `StartManualIdentityExchangeUseCase`
- `ReceiveIdentityExchangeUseCase`
- `AcceptIdentityExchangeUseCase`
- `ReceiveIdentityExchangeAcceptedUseCase`
- `ReceiveIdentityReadyUseCase`
- `ReceiveIdentityAcknowledgementUseCase`
- `EstablishMutualIdentityUseCase`
- `CloseIdentityExchangeUseCase`
- `InvalidateIdentityExchangeUseCase`
- `RecoverIncompleteIdentityUseCase`
- `RecoverManualIdentityExchangeUseCase`

Cross-feature decisions around invitations/conversations are made by `ConversationFlowHandler`, not inside the identity repository.

## Backup, restore and key replacement

The current code contains a full encrypted backup/restore path and a separate remote-key replacement review/reconnection path. These are documented in detail in [Identity backup, recovery and reconnection](identity-recovery.md).

Important classes include:

- `PrepareIdentityBackupUseCase`, `RestoreIdentityBackupUseCase`
- `IdentityBackupRepositoryImpl`, `AndroidIdentityBackupCodec`
- `PendingRemoteIdentityChange`
- `StagePendingRemoteIdentityChangeUseCase`
- `ApprovePendingRemoteIdentityChangeUseCase`
- `ApprovedIdentityReconnection`
- `ApprovedIdentityReconnectionRepositoryImpl`
- `ApprovedIdentityReconnectionObserver`
- `ApprovedReconnectionRetryWorker`
- `StartRecoveryInvitationUseCase`

## Sharing/import

`CreateSharedIdentityUseCase`/sharing UI create the shareable representation. Scanning/import lives in `:feature:contactimport`. Importing a public identity does not itself mean the peer is verified or authorized for Direct messaging.

## Platform status

Android has the production-shaped private-key/backup platform implementations. iOS source-set adapters exist for several contracts, but the iOS application as a whole remains incomplete and should not be described as Android-parity.
