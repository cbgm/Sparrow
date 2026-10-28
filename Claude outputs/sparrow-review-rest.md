# Sparrow review — everything outside conversation orchestration

Review date 2026-10-28, branch pv-server (HEAD 926581c8). Read-only review by area; every finding was traced through the code.
Severity: **H** high, **M** medium, **L** low. Paths are shortened; the module is always given.

## Top priorities (fix before going live)

1. **Server — anyone can sign in to the gateway as any user** (H) — `server/gateway GatewaySessionHandler.kt:199`: a `Register`
   without proof fields is ACCEPTED. Attacker registers a victim's routing ID, receives the victim's pending and live envelopes,
   acknowledges (deletes) them and sends as the victim. Fix: require the signed proof on every Register.
2. **Server — anyone can register a push device for any routing ID** (H) — `server/push PushRoutes.kt:48-90`: attacker binds
   their FCM token to the victim's ID, gets the wake-up ID, reads `/push/wake/{id}/inbox` and acks (deletes) it. Rows never
   expire. Fix: signature by the routing key over (token, time) + TTL.
3. **Server — phone aliases can be taken over without verification** (H) — `server/presence-directory PhoneVerification.kt:28`:
   a route with phone aliases and no token is accepted; a higher generation evicts the real owner. With no Firebase configured,
   every alias is accepted. Fix: never accept an alias without proof (short-lived binding in Redis only).
4. **Server — node trust is open** (H/M) — `server/node-registry NodeRegistryStore.kt:120`: the registry signs whatever
   capabilities (GATEWAY/MAILBOX/FEDERATION) a self-signed node claims via public `POST /v1/nodes`;
   `server/security RegistryCertificateSignatures.kt:36` takes the root from the certificate and ignores validity dates.
   Fix: operator approval of capabilities, pinned root, time checks.
5. **App lock is skipped** (H) — after process death the restored back stack never shows Startup/AppLock
   (`startup StartupViewModel.kt:43-47`, `navigation AppNavigation.kt:76,82`; startup tasks are skipped too), and it never
   re-locks after backgrounding (`shared App.kt:33-40`, `feature/applock AppLockViewModel.kt:52-60`). Fix: lock state above the
   NavHost, re-lock on ON_STOP (optional grace), secure the window while locked.
6. **A transient keystore/crypto error wipes identity and database** (H) — `shared ResolveIdentityStatusStartupTask.kt:18-20`,
   `feature/identity IdentityRepositoryImpl.kt:51,71-72`: any exception in `hasIdentityPrivateKeys()` → INCOMPLETE →
   automatic `RecoverIncompleteIdentity` → `clearAllTables()` + key reset, no prompt. Fix: only "positively absent" is
   INCOMPLETE; never auto-wipe.
7. **Contact spoofing via display name** (H) — `incoming VerifyIncomingContactInvitationUseCase.kt:20` uses the inviter's
   display name as phone number → `IdentityPeerRepositoryImpl.kt:97` fuzzy match files a stranger's invite under an existing
   contact (skips "block unknown invites", saves the claimed number). Fix: never derive a phone number from the display name.
8. **Blocklist bypass and wipe** (H) — `feature/contacts ContactRepositoryImpl.kt:291` exact-match lookup creates a second
   blocked contact; an incoming invite matches the unblocked one and `applyMerge` (`IdentityPeerRepositoryImpl.kt:43`) deletes
   the blocked duplicate with its block, before the block check runs. Fix: same equivalence for blocking; never merge away a
   blocked contact.
9. **One malformed envelope kills the WebSocket session** (H) — `feature/transport GatewayServerMessageHandler.kt:29-35`:
   decode/validation errors end the session; a crafted envelope in push storage is replayed on every reconnect → no messages
   for up to 7 days; same for the pending-envelope HTTP list. Fix: per-frame error handling, ignore unknown types.
10. **Gateway ack timeout stalls the outbox** (H) — `feature/transport GatewayPendingRequestRegistry.kt:26,40`: a timeout is
    returned as `TimeoutCancellationException`, rethrown as cancellation by `safeSuspendCall` / `DefaultOutboxProcessor.kt:77`;
    items stay PROCESSING, parallel sends are cancelled, the runner's observation job ends. Every ENVELOPE_REJECTED triggers
    it (server errors carry no envelope ID). Fix: plain exception for timeouts, envelope ID in gateway errors.
11. **Group epochs** (H) — `feature/membership`: a welcome more than one epoch ahead is rejected, not deferred
    (`GroupWelcomeSecurity.kt:37-42`) → a member can be cut off for good; rotation is committed before the welcomes are queued
    (`GroupEpochSecurityDataSource.kt:60-113` + callers) → crash/enqueue failure loses the welcome; previous-epoch messages are
    lost on every membership change (`GroupSecurityManager.kt:398`, old key deleted). Fix: accept/defer higher epochs, enqueue
    welcomes in the same transaction, keep the previous epoch key for a grace window.
12. **Invites go out in plaintext with name and profile photo** (H, privacy) — `delivery OutgoingPacketTransportPolicy.kt:36-46`
    + `feature/identity LocalProfilePictureDataSource.forInvite()`: relays store name, photo (up to 1 MB), group title and keys
    for up to 7 days. The acceptance could be sealed (inviter key known). Fix: seal when a key exists, drop name/photo from
    plaintext packets.

## Privacy (owner rule: server remembers nothing beyond delivery, no IP exposure)

- (H) `server/mailbox MailboxRoutes.kt:65`, `PostgresMailboxStore.kt:36`: `owner_key_hash` = unsalted SHA-256 of the client
  IP, stored permanently (IPv4 reversible in seconds); mailbox and envelope lifetimes are sender-chosen and unbounded.
- (M-H) `server/observability ServerObservability.kt:39` + all compose files: CallLogging logs full URIs
  (`/v1/routes/{routingId}`, `/push/wake/{id}/inbox`, blob/mailbox IDs); Docker json-file logs grow forever.
- (M) `feature/transport Sha256RoutingIdGenerator.kt:9-23`: `scphone1_` = unsalted SHA-256 of the phone number, signed into
  every route registration next to the routing ID → any node links phone number, routing ID and IP.
- (M) `server/presence-directory PresenceRoutes.kt:123`: public, unlimited `GET /v1/routes/{id}` → enumerate which numbers use
  Sparrow, their device identity and online status.
- (M) `feature/identity FirebasePhoneVerificationRepository.kt:120-127`: Firebase persists the signed-in user incl. tokens in
  SharedPreferences until signOut (process death leaves it); every verification leaves a permanent Firebase Auth user record
  (`server/presence-directory FirebasePhoneTokenVerifier.kt:15` never deletes it).
- (M) Link previews: every URL in received group messages is sent to the node automatically; node caches URL→preview 6 h,
  unauthenticated (`feature/linkpreview RemoteLinkPreviewDataSource.kt:25`, `server/link-preview LinkPreviewService.kt:31-38`).
- (M) Model downloads go straight to third parties with the user's IP: Whisper from huggingface.co
  (`feature/voice AndroidWhisperModelStore.kt:48-56`), embedding model from storage.googleapis.com
  (`feature/embedding LocalEmbeddingModel.kt:8`).
- (M) `feature/ocr ReceiptScanRepositoryImpl.kt:20,22`: full receipt text logged at debug level, also in release builds.
- (M) `feature/notification SparrowNotificationManager.kt:62`: VISIBILITY_PRIVATE without a public version and no preview
  setting → sender and plaintext on the lock screen by default.
- (M) `feature/settings DisclaimerContent.kt`: privacy texts contradict behaviour (link previews, push token storage/FCM not
  mentioned, promised diagnostics toggle and account deletion don't exist, backup placeholder, `[DATE]`/`[CONTACT EMAIL]`).
- (M) `androidApp AndroidManifest.xml:17` `usesCleartextTraffic="true"` in release; `ControlPlaneSettingsViewModel` accepts
  `http://` control planes and an unsigned JSON list over HTTP (and the validation lives in the ViewModel).

## Server / installer

- (M) Gateway/presence have no rate or resource limits: unlimited dispatcher channel per recipient key
  (`GatewaySessionWorkDispatcher.kt:33`), blob store fillable with trivial tickets, full ticket size reserved before streaming,
  no read timeout.
- (M) Link preview can OOM the gateway (`LinkPreviewFetcher.kt:156` ImageIO without dimension check, `:68` unbounded body)
  and allows DNS-rebinding SSRF into the Docker network.
- (M) `control-plane docker-compose.production.yml:6` publishes the plain-HTTP Caddy site on 0.0.0.0 in public mode.
- (M) `installer update_server.py:252-262`: a failed update leaves new tag/code without rollback; releases are checksummed but
  not signed.
- (L) `control-plane bootstrap-control-plane.sh:203-215`: plaintext secrets copied into `.env.runtime(.previous)`;
  `InternalApiAuthentication` allows everything when no token is configured.

## Transport / messaging

- (M) `ControlPlaneCandidateVerifier.kt:31-33`: with no pinned root a self-signed directory passes; `http://` hints accepted →
  a malicious node can inject a control plane trusted on first use.
- (M) `PlatformHttpClient.*:15` `expectSuccess = true` makes all status checks dead code (mailbox 404/410/401 "gone" handling
  never runs; push-token 4xx marks a healthy control plane unreachable).
- (L/M) `ClientPresenceRouteCoordinator.kt:215-217` signs the nodeId from unauthenticated `/v1/gateway/info`.
- (L) Data race in `FailedNodeTracker` (plain map, several coroutines); no reconnect backoff after clean closes; `runCatching`
  instead of `safeSuspendCall` across transport/protocol (root cause of #10); envelope/wake-up IDs interpolated into URLs
  without encoding (`HttpMailboxGateway.kt:64` — `..` would hit `DELETE /v1/mailboxes/{id}`).

## Identity / onboarding / crypto

- (M) "Verify" marks whatever keys are stored at tap time, not the ones the user compared
  (`IdentityVerificationDataSource.kt:223-233`).
- (M) Imported identity keys are never length-checked (`IdentityShareRepositoryImpl.kt:269-275`) → a malformed QR can pin a
  broken key as VERIFIED.
- (L/M) Phone hint picker / SIM auto-read re-fire on re-composition and overwrite the typed number
  (`OnboardingPermissions.android.kt:65`, `PhoneNumberHintLauncher.android.kt:57-62`).
- (L) `createIdentity` rollback skipped on cancellation (`IdentityRepositoryImpl.kt:117-129`) → feeds #6; `runCatching` in
  backup/exchange/reset code; restore password kept as an immutable String in UI state.

## Contacts / invite / import

- (H) Device contact import deletes a contact's numbers, then throws on short codes ("112") and aborts the whole import
  (`ContactRepositoryImpl.kt:533,548`, `ImportDeviceContactsUseCase.kt:72`); no transaction.
- (M-H) A sender's self-claimed number overwrites the stored preferred number (`ContactRepositoryImpl.kt:226-250`).
- (M-H) Shared-identity import matches by phone before key; unsigned payload; renames the contact before the key check fails
  (`ContactRepositoryImpl.kt:491`, `IdentityShareRepositoryImpl.decode`).
- (M) "Verify by QR" re-imports whatever QR is scanned instead of comparing it (`VerifyContactByQrUseCase.kt:38`).
- (M) `DirectConversationViewModel.kt:238` passes the contact ID as `scannedIdentity` (`AppRoute.ImportContact(contactId)`).
- (M) Unauthenticated envelopes create permanent placeholder contacts (`ContactByRoutingIdDataSource.kt:36`) → contact-list
  flooding.
- (M) Merges/reconciliation `contactDao.deleteById` cascade-delete conversations and outbox rows.
- (M) National vs international number formats never match exactly (`DefaultPhoneNumberNormalizer.kt`); raw number stored as
  `normalizedValue` (`IdentityPeerRepositoryImpl.kt:73`).
- (L) Invitation accept/decline/block errors only logged (`InvitationViewModel.kt:162`).

## Membership / database

- (H if any build ≤54 shipped, else L — app not live) No migration 54→55 (`SparrowDatabase.kt:131`, `DatabaseBuilder.kt:16`);
  43→53 migrations unregistered; 40→41 missing.
- (M) Admin leave/delete never deletes group keys from the key store; the departed admin knows the N+1 key and the promoted admin
  never re-rotates (`GroupLeaveDataSource.kt:229-236`, `GroupMembershipDeletionDataSource.kt:85,128`).
- (M) Two admins rotating concurrently split the group (deterministic welcome IDs, `GroupWelcomeSecurity.kt:90-121,174-190`).
- (L) `updateStatus` results ignored in removal/leave; incoming removal `complete()` doesn't check the membership's group;
  `runCatching` in membership data sources.

## Attachments / media / voice / avatar

- (H) Voice recording keeps running after leaving the chat; the next chat's composer can send the previous chat's audio
  (`VoiceComposerViewModel.kt:16`, `MessageControl.kt:228-270`).
- (H) Failed uploads leave plaintext cache files forever (`MessageAttachmentOperationsRepositoryImpl.kt:110-133,390-407`).
- (M-H) Concurrent `loadBytes` for one part writes several plaintext files, keeps one (`MessageAttachmentDataSource.kt:170-194`).
- (M-H) Whole blobs in memory with ~4-5x amplification (96 MB file ≈ 400 MB heap); incoming sizes are sender-declared up to
  128 MB × 8 parts allocated up front (`BlobTransferDataSource.kt:38-145`).
- (M) Avatar decode at full resolution (30000² PNG → GBs) eagerly in the chat list (`AvatarRepositoryImpl.kt:63`); one decode
  failure kills the cached flow forever (`:38-46`).
- (M) No max recording length; PCM buffered and copied twice (`AndroidVoiceRecorder.kt:62-93`); stop failure leaves RECORDING.
- (L/M) Storage management can delete the only copy of not-yet-uploaded outgoing attachments.
- (L) `catch (Throwable)` / `runCatching` swallow cancellation in delete loops and media export.

## Expenses / payment / polls / OCR

- (H) Polls can be sent into the wrong group (one app-wide `MutableStateFlow<Poll?>`, no groupId, cleared only on success —
  `PollComposerRepositoryImpl.kt:46`, `GroupPollController.kt:35,52-60`).
- (H) Editing an expense deletes its receipt when the receipt wasn't downloaded locally (`ExpenseCreationMapper.kt:82-110`).
- (M) Expense requests can be dropped with two group screens alive (one global Channel, `ExpenseComposerRepositoryImpl.kt:21,39`).
- (M) Settle-up offers the full amount again while a SENT payment is unconfirmed (`ExpenseOverviewUiMapper.kt:243-246`).
- (L) Offline exchange-rate refresh shows a global error snackbar; draft discarded on Send when only receipt/custom split set;
  zero-decimal currencies scanned wrong (¥1,234 → 12).

## Chats / notifications / settings / app shell

- (H) Expedited FCM sync worker without `getForegroundInfo()` → fails/crashes on Android 10/11, no background delivery
  (`PendingMessageSyncScheduler.kt:34-36`).
- (M-H) `ExistingWorkPolicy.REPLACE` cancels a running sync → messages stored but never notified.
- (M) Notification preview shows the newest *text* message, not the newest message (old or own text for photos/voice).
- (M) "Clear local data" only clears settings incl. the blocklist; messages/keys stay; blocks silently vanish after restart.
- (M-L) Mailbox provisioning retry loop inside a StateFlow collector swallows reconnects (`ForegroundConnectionHandler.kt:71-76`).
- (L) `DirectConversationViewModel` shared flow without catch → crash on Room/mapper errors; group conversation catch placed
  after `flatMapLatest` → permanent Failed; `RecoveryInboxViewModel` multi-step rule in the ViewModel, not atomic.

## Safety / search / local AI / app lock

- (H) Edited messages keep their old safety assessment → "hi" then edit to a phishing link shows no warning
  (`ChatDao.kt:323-352`, `MessageSafetyDao.kt:29`). Same for search embeddings (M).
- (M) A message deleted during indexing leaves semantic search Failed for the session (FK violation).
- (M) Embedding model URL points to `latest` but the hash is pinned; a mismatch leaves the partial file → fails forever.
- (L-M) Model reloaded after disable; inference blocks Default dispatcher threads; watchers stop on one DB error;
  disable races with indexing; every search loads all embeddings with full text; results without a contact name can't open.

## Checked and found fine (summary)

Blob crypto (random key/nonce, AAD, hash before decrypt); backup format (PBKDF2 310k, AES-GCM, wiping); keystore wrapping;
sealed box / XChaCha use; exchange replay and pinning checks; identity-change approval CAS; in-memory phone token with expiry;
directory/certificate verification on the client; invitation and revocation signing; mailbox capability checks under row
locks; constant-time comparisons; blob ID validation; Caddy without access logs; money math and overflow guards; SEPA/IBAN;
bank details in Keystore; auto-reply claim (no ping-pong); biometric prompt setup; notification deep links; composer draft
persistence; paging maths.
