# Identity backup, recovery and reconnection

This page documents the current identity recovery implementation, including encrypted backup/restore, remote key replacement review, persistent approved reconnection, and retry after process/network interruptions.

## Four different concepts

Do not collapse these into one “recovery” action:

1. **Identity backup/export** — copy the current public identity plus both private keys into an encrypted backup document.
2. **Identity restore** — restore those exact cryptographic keys on a fresh local installation that has no identity yet.
3. **Remote identity replacement** — a contact now presents different keys; the old trust binding must not be silently overwritten.
4. **Conversation reconnection** — explicitly resume/re-authorize an existing peer relationship after an approved change or missing authorization.

## Identity backup

Production classes:

- `PrepareIdentityBackupUseCase`
- `MarkIdentityBackupExportedUseCase`
- `GetIdentityBackupStatusUseCase`
- `ReadIdentityRestoreDocumentUseCase`, `ReadIdentityBackupUseCase`, `SaveRestoredIdentityUseCase`
- `IdentityBackupRepository`
- `IdentityBackupRepositoryImpl`
- `IdentityBackupCodec`
- `AndroidIdentityBackupCodec`
- `IdentityBackupStatusDataSource`
- `IdentityBackupDocumentLauncher`
- `IdentityBackupSection`
- `IdentityBackupPasswordDialog`
- `IdentityBackupViewModel` (export)
- `IdentityRestoreViewModel`, `IdentityRestoreDialog` (restore during onboarding)

`IdentityBackup` contains `PublicIdentity`, the encryption private key and the signing private key. The model is explicitly documented in code as short-lived sensitive data and must never be logged or persisted unencrypted.

`PrepareIdentityBackupUseCase` requires a password of at least 12 characters, verifies the identity is `READY`, loads both private keys, encrypts through `IdentityBackupRepository`, and zeroes the in-memory private-key arrays in a `finally` block.

```mermaid
sequenceDiagram
    participant UI as IdentityBackupViewModel
    participant PREP as PrepareIdentityBackupUseCase
    participant ID as IdentityRepository
    participant CODEC as AndroidIdentityBackupCodec
    participant SAF as IdentityBackupDocumentLauncher
    participant MARK as MarkIdentityBackupExportedUseCase

    UI->>PREP: password
    PREP->>ID: public identity + private keys
    PREP->>CODEC: encrypt IdentityBackup
    CODEC-->>UI: encrypted document bytes
    UI->>SAF: write document
    SAF-->>UI: document closed successfully
    UI->>MARK: expected public keys
    MARK->>ID: verify identity did not change during export
```

The backup status is keyed to the current signing+encryption public keys and is represented by `IdentityBackupState` (`NOT_BACKED_UP`, `EXPORTED`, `IMPORTED`).

## Restore after reinstall

Restore is part of onboarding: on the phone page the user enters phone number and name and taps
*Restore*. `IdentityRestoreViewModel` then runs three stages, and nothing is stored before the
phone number has been verified:

1. **Read** — the user selects the backup file; `ReadIdentityRestoreDocumentUseCase` reads it
   (at most 16 KiB) and deletes the temporary copy.
2. **Decrypt and check** — after the password is entered, `ReadIdentityBackupUseCase` refuses to
   continue if a local identity already exists, decrypts/authenticates the backup and validates
   the key pairs (`IdentityRepository.validateIdentityKeys(...)`), all in memory.
3. **Verify and save** — the phone verification dialog runs (see
   [Onboarding](onboarding.md#phone-verification)). Only after it succeeds does
   `SaveRestoredIdentityUseCase` save the local phone/name profile, call
   `IdentityRepository.restoreIdentity(...)` with the original public identity and private keys,
   and mark the backup as imported.

Cancelling the verification discards the decrypted backup. Private-key byte arrays, the document
bytes and the password are wiped after use.

```mermaid
sequenceDiagram
    participant UI as IdentityRestoreViewModel
    participant READ as ReadIdentityBackupUseCase
    participant VER as PhoneVerificationDialog
    participant SAVE as SaveRestoredIdentityUseCase
    participant ID as IdentityRepository
    participant CODEC as IdentityBackupRepository

    UI->>READ: backup bytes + password
    READ->>ID: require NOT_CREATED
    READ->>CODEC: decrypt/authenticate
    READ->>ID: validateIdentityKeys(...)
    READ-->>UI: IdentityBackup (in memory)
    UI->>VER: phone number
    VER-->>UI: Verified
    UI->>SAVE: backup + phone + name
    SAVE->>ID: restoreIdentity(public, private encryption, private signing)
    SAVE->>CODEC: markImported(restored identity)
```

The backup restores identity key material; it is not a database backup of conversations/messages/membership rows.

## Incomplete identity recovery

`RecoverIncompleteIdentityUseCase` and `RecoverManualIdentityExchangeUseCase` recover interrupted identity setup/exchange state. `IdentityExchangeDataSource` persists the exchange state machine and contains replay helpers such as `recoverIncomingInviteReplay()`, `queueAcceptanceReplay()` and `queueReadyReplay()`.

## Detecting a changed remote identity

Relevant types:

- `PendingRemoteIdentityChange`
- `PendingRemoteIdentityChangeEntity`
- `PendingRemoteIdentityChangeDao`
- `PendingRemoteIdentityChangeDataSource`
- `PendingRemoteIdentityChangeRepository`
- `StagePendingRemoteIdentityChangeUseCase`
- `ApprovePendingRemoteIdentityChangeUseCase`
- `DeclinePendingRemoteIdentityChangeUseCase`
- `DismissPendingRemoteIdentityChangeUseCase`
- `ObservePendingRemoteIdentityChangesUseCase`
- `RemoteIdentityReplacementRequiredException`

When a packet would replace the key material bound to an existing peer, the incoming workflows (for example `ReceiveContactInviteWorkflow`, `ReceiveGroupInviteWorkflow`, `ReceiveGroupJoinRequestWorkflow`) stage a `PendingRemoteIdentityChange` instead of silently accepting the replacement. This creates an explicit review point.

## Recovery inbox

`navigation.presentation.inbox.RecoveryInboxViewModel` observes pending identity changes and exposes the review actions in the application mailbox/inbox experience. `InvitationViewModel` receives the current recovery-request count so the inbox is not automatically closed while identity-review work remains.

This is intentionally adjacent to invitations in the UI but is a different persisted domain object.

## Approval becomes persistent reconnection work

Approving a pending key change creates an `ApprovedIdentityReconnection` persisted through:

- `ApprovedIdentityReconnectionEntity`
- `ApprovedIdentityReconnectionDao`
- `ApprovedIdentityReconnectionDataSource`
- `ApprovedIdentityReconnectionRepositoryImpl`
- `ObserveApprovedIdentityReconnectionsUseCase`
- `GetApprovedIdentityReconnectionUseCase`
- `AcknowledgeQueuedRecoveryInvitationUseCase`

Database migrations `PendingRemoteIdentityChangeMigration49To50`, `PendingRemoteIdentityChangeMigration50To51`, `ApprovedIdentityReconnectionMigration51To52` and `ApprovedIdentityOfferMigration52To53` show that this is durable state, not an in-memory UI event.

## App-lifetime retry

`StartApprovedIdentityReconnectionObserverStartupTask` starts `ApprovedIdentityReconnectionObserver` after local identity readiness as part of the task-based application runtime. The observer keeps one worker per approved peer and starts `ApprovedReconnectionRetryWorker`.

```mermaid
sequenceDiagram
    participant APP as Startup runtime
    participant OBS as ApprovedIdentityReconnectionObserver
    participant DB as approved reconnection repository
    participant WORK as ApprovedReconnectionRetryWorker
    participant START as StartRecoveryInvitationWorkflow
    participant FLOW as AcceptApprovedIdentityChangeWorkflow / RequestReauthorizationWorkflow / ResumeApprovedGroupJoinWorkflow

    APP->>OBS: run()
    OBS->>DB: observe approvals
    DB-->>OBS: ApprovedIdentityReconnection
    OBS->>WORK: run(approval)
    loop until completed / superseded
        WORK->>DB: approval still current?
        WORK->>START: enqueue/resume recovery (WorkflowEngine)
        START->>FLOW: original approval or reauthorization path
        WORK->>DB: acknowledge when fresh authorization exists
    end
```

A transient Room observation failure is retried with capped exponential delay. The approved row remains until the recovery invitation has been queued/authorization becomes fresh, allowing recovery to survive process restart.

## `StartRecoveryInvitationWorkflow`

This workflow (`:feature:conversationorchestration:lifecycle`, step chain `FindGroupMembership -> ResumeGroupJoin | EnsureConversation -> AcceptOriginalInvitation | RequestReauthorization`) carefully distinguishes Direct recovery from Group membership recovery:

- If the approval maps to a `MembershipHandshake`, it resumes the Group path.
- A member-side Group offer remains in the mailbox for normal Group acceptance; it does not create a Direct invitation.
- An owner-side changed-key Group JOIN resumes through `ResumeApprovedGroupJoinWorkflow`.
- IDs with the `group-membership-` prefix are never converted into Direct recovery merely because the handshake disappeared.
- Direct recovery can create a conversation shell via `DirectConversationRepository.getOrCreate(peerId)` (`EnsureConversation` step), but that shell alone does **not** grant transport authorization or flush queued messages.
- If the original invitation challenge/keys/timestamps are still complete and unexpired, `AcceptApprovedIdentityChangeWorkflow` resumes that exact flow.
- Otherwise `RequestReauthorizationWorkflow` starts a fresh authorization request.

## Explicit reconnection from an existing conversation

`DirectConversationViewModel` submits `DirectConversationCommand.ReconnectConversation(contactId)`, executed by `ReconnectDirectConversationWorkflow` (`CheckReconnectable -> PreflightRouting -> LoadAuthorization -> [RevokeExchange] -> StartInvitation`). This is the explicit “reconnect contact” path: the existing conversation can survive locally while authorization is renewed.

## Outgoing safety gate

Recovery approval does not bypass normal encryption requirements. The outgoing identity-review gate in `OutgoingPacketSender` (`blocksDirectPacketDuringIdentityReview`) and `RequireDirectChatAuthorizationUseCase` prevent Direct application messages from being released until the required established identity/authorization state exists. `SendDirectMessageWorkflow` keeps messages persisted as `WAITING_FOR_AUTHORIZATION` while an identity change is pending or authorization is missing; `ReleaseWaitingDirectMessagesWorkflow` releases them once authorization is valid.

## Why a second normal invitation is a bug

An approved key replacement is supposed to resume the approved original identity/reconnection workflow. It is **not** supposed to generate an unrelated ordinary invitation after the conversation has already been established. The code therefore carries original invitation challenge/timestamps/key material in `ApprovedIdentityReconnection` and has explicit Group-vs-Direct guards in `StartRecoveryInvitationWorkflow`.

## Local phone number

The current snapshot has `LocalIdentityProfileDataSource`, `GetLocalPhoneNumberUseCase`, `NormalizeLocalPhoneNumberUseCase`, `SaveLocalPhoneNameUseCase` and the initial identity/onboarding phone editor. The number is verified by SMS once, when the identity is created or restored (see [Identity](identity.md#phone-verification-at-sign-up)). It does **not** contain a completed post-creation phone-number migration/broadcast workflow that rewrites existing peer relationships. Documentation should not claim that such migration is implemented until dedicated production classes exist for it.
