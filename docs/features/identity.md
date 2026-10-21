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
- `IdentityCreateViewModel` (create from the onboarding phone page)

The public identity contains the public encryption/signing keys plus public profile metadata needed by the protocol. Private keys stay local except when the user explicitly exports an encrypted identity backup.

## Phone verification at sign-up

The phone number is verified by SMS once, when the identity is created or restored during
onboarding (see [Onboarding](onboarding.md#phone-verification)). There is no later
re-verification and no lasting login.

Key types:

- `PhoneVerificationViewModel`, `PhoneVerificationDialog`, `PhoneVerificationHost`
- `SendPhoneVerificationCodeUseCase`, `ConfirmPhoneVerificationCodeUseCase`
- `PhoneVerificationRepository` and `PhoneVerificationStep` (`CodeSent`, `CodeReceived`,
  `Verified(token)`)
- `FirebasePhoneVerificationRepository` (Android)
- `IdentitySetupDialogs` (restore + verification dialogs for the onboarding screen)

`PhoneVerificationRepository.sendCode(...)` returns a `Flow<Result<PhoneVerificationStep>>`; the
Android implementation wraps Firebase phone authentication in a `callbackFlow`. After the code is
confirmed (or the platform verified the number by itself), the app signs in with the phone
credential only to obtain a one-time Firebase ID token and signs out again immediately.

The dialog only verifies. `IdentityCreateViewModel` creates the keys, and
`IdentityRestoreViewModel` saves the restored backup, only after the result is `Verified`.

The token is kept **in memory only** (`LocalIdentityProfileDataSource.keepPhoneVerificationToken`,
lifetime 50 minutes) and is never persisted. `ClientPresenceRouteCoordinator` in
`:feature:transport` sends it once as `ClientRouteRegistration.phoneVerificationToken` with the
first presence-route registration that advertises phone aliases, and clears it afterwards (also
when that registration fails). The presence directory verifies the token with Firebase and
checks that the verified number matches the advertised phone alias. It keeps verified tokens only
in a bounded in-memory cache until they expire and persists nothing about the verification. A
registration without a token is accepted; the verification is part of sign-up, not of every
connection.

Debug builds: `DEBUG_SMS_CODE` in `local.properties` switches Firebase to test mode for the
number, and the dialog offers *Skip (debug)*, which continues without a token. See
[Local development](../development/local-development.md).

iOS has no `PhoneVerificationRepository` implementation yet.

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

Cross-feature decisions around invitations/conversations are made by the workflows in `:feature:conversationorchestration:lifecycle` / `incoming` (for example `HandleIdentityResultWorkflow`, `ReceiveContactInviteWorkflow`, `CompleteManualIdentityWorkflow`), not inside the identity repository.

## Backup, restore and key replacement

The current code contains a full encrypted backup/restore path and a separate remote-key replacement review/reconnection path. These are documented in detail in [Identity backup, recovery and reconnection](identity-recovery.md).

Important classes include:

- `PrepareIdentityBackupUseCase`, `IdentityBackupViewModel`
- `ReadIdentityBackupUseCase`, `SaveRestoredIdentityUseCase`, `IdentityRestoreViewModel`
- `IdentityBackupRepositoryImpl`, `AndroidIdentityBackupCodec`
- `PendingRemoteIdentityChange`
- `StagePendingRemoteIdentityChangeUseCase`
- `ApprovePendingRemoteIdentityChangeUseCase`
- `ApprovedIdentityReconnection`
- `ApprovedIdentityReconnectionRepositoryImpl`
- `ApprovedIdentityReconnectionObserver`
- `ApprovedReconnectionRetryWorker`
- `StartRecoveryInvitationWorkflow` (in `:feature:conversationorchestration:lifecycle`)

## Sharing/import

`CreateSharedIdentityUseCase`/sharing UI create the shareable representation. Scanning/import lives in `:feature:contactimport`. Importing a public identity does not itself mean the peer is verified or authorized for Direct messaging.

## Platform status

Android has the production-shaped private-key/backup platform implementations. iOS source-set adapters exist for several contracts, but the iOS application as a whole remains incomplete and should not be described as Android-parity.
