# Identity

`:feature:identity` owns local cryptographic identity, identity exchange/trust state, encrypted identity backup/restore, remote identity-change review and approved reconnection persistence.

## Core identity

Key types include:

- `IdentityRepository` / `IdentityRepositoryImpl`
- `DirectIdentitySetupModeRepository` / `DirectIdentitySetupModeRepositoryImpl` with `DirectIdentitySetupModeLocalDataSource`: whether a new direct contact gets the identity with the invitation or by hand; Settings only shows and changes it
- `LocalIdentityKeyDataSource`: the device's keys for every other module (`LocalPublicIdentitySource`, `LocalSigningKeyPairSource`, `LocalEncryptionKeyPairSource`, `LocalSigningPublicKeySource`)
- `PublicIdentity`
- `CreateIdentityUseCase`
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
- `ReservePhoneVerificationSlotUseCase`, `PhoneVerificationSlotRepository` /
  `PhoneVerificationSlotRepositoryImpl` and `PhoneVerificationSlotStatus` (`Reserved(resends)`,
  `DayFull(retryAt)`), over `PhoneVerificationSlotSource` in `:protocol`
- `SendPhoneVerificationCodeUseCase`, `ConfirmPhoneVerificationCodeUseCase`
- `PhoneVerificationRepository` and `PhoneVerificationStep` (`CodeSent`, `CodeReceived`,
  `Verified(token)`)
- `FirebasePhoneVerificationRepository` (Android)
- `IdentitySetupDialogs` (restore + verification dialogs for the onboarding screen)

`PhoneVerificationRepository.sendCode(...)` returns a `Flow<Result<PhoneVerificationStep>>`; the
Android implementation wraps Firebase phone authentication in a `callbackFlow`. After the code is
confirmed (or the platform verified the number by itself), the app signs in with the phone
credential only to obtain a one-time Firebase ID token and signs out again immediately.

Before the first SMS the dialog reserves a sign-up slot at the phone verifier
(`PhoneVerificationSlotSource`, implemented by `PhoneAttestationRemoteDataSource` in `:feature:transport`).
The request carries only a small proof of work (`MailboxCreationWorkSolver`, 18 bits) and a reply key, sealed
for the verifier's request key and sent through the relays like the attestation; during onboarding the signed
node directory is loaded first (`NodeDirectoryRelayLoader`), so the verifier never sees the app's address. The
answer is a random slot token (valid for an hour) and how often the code may be sent again, or `429` with the
start of the next UTC day: then no SMS is sent. A Directory without phone verification offers no slot
(`NotOffered`): the SMS is sent and the number stays unverified. The slot is kept in memory with the token
(`LocalIdentityProfileDataSource.keepPhoneVerificationSlot`) and the attestation uses it up.

The dialog only verifies. `IdentityCreateViewModel` creates the keys, and
`IdentityRestoreViewModel` saves the restored backup, only after the result is `Verified`.

The token is kept **in memory only** (`LocalIdentityProfileDataSource.keepPhoneVerificationToken`,
lifetime 50 minutes) and is never persisted. `AliasClaimRegistrar` in `:feature:transport` sends it
once, sealed and through two relays, to the phone verifier next to the Control Plane Directory
(`PhoneAttestationRemoteDataSource`), which checks it with Firebase and answers with an attestation
signed under the pinned Directory key: this phone alias belongs to this device. The attestation goes
into the alias claims (`AliasClaim.phoneAttestation`), stays in memory only, and the token is cleared
once a Control Plane answered. Every presence directory checks the attestation; none sees the token
or the number. The verification is part of sign-up, not of every connection.

Debug builds: `DEBUG_SMS_CODE` in `local.properties` switches Firebase to test mode for the
number, and the dialog offers *Skip (debug)*, which continues without a token or slot. See
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
