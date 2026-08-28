# Current feature status

This page describes what is implemented in the current codebase. It is deliberately conservative: roadmap items are not listed as working features.

## Platform status

| Area | Status |
|---|---|
| Android client | Current usable development/product target |
| iOS client | KMP/Xcode structure exists, but major runtime/platform functionality is incomplete; not usable/supported |
| Control Plane | Implemented as Docker/Ktor services + Windows launcher bundle |
| Community Node | Implemented as Docker/Ktor services + Windows and macOS/Linux launcher bundles |
| Official tagged release | **Not published yet**; workflow/package format is implemented |

## Android application

### Onboarding and identity

- welcome/privacy/phone/permission onboarding;
- local encryption + signing identity creation;
- Android-protected private-key persistence;
- public identity sharing;
- QR/manual identity import paths.

### Contacts and trust

- device-contact import/linking;
- phone normalization/merge behavior;
- contact invitations inbox;
- accept/decline/decline+block;
- block/unblock management;
- public identity exchange/acknowledgement;
- contact details/security state;
- safety-number and QR verification flows.

### Direct chats

- persistent conversations/messages;
- encrypted transport payload path;
- persistent outgoing outbox and retry;
- sent/delivered/read state;
- unread/read handling;
- typing indicator;
- identity/security-state handling;
- conversation deletion/authorization-revocation behavior;
- messages queued during automatic re-invitation are released on acceptance, discarded on decline, and expire after two days;
- text plus typed image/video/file/location/contact message parts.

### Group chats

- group creation and invitation flows;
- membership activation and security key distribution;
- add/remove members;
- multiple admins/member promotion;
- leave/admin-transfer requirements;
- group verification snapshots/receipts;
- epoch-based group encryption state;
- one outgoing packet per current active recipient;
- per-recipient delivery/read aggregation;
- group typing indicators;
- re-invitation through a new active membership period;
- the same typed text/image/video/file/location/contact message content used by Direct chats.

There is no orphaned-group mode in the current architecture.

### Attachments and media

Sparrow currently supports attachment types `IMAGE`, `VIDEO`, `FILE`, `LOCATION`, and `CONTACT`. All five use the encrypted blob attachment transport.

- up to 8 selected attachments per normal media/file message; location/contact actions are single-shot attachment messages;
- image limit: 4 MiB per image;
- video limit: 64 MiB per video;
- file limit: 96 MiB per file;
- total selected attachment payload limit: 96 MiB per message;
- gallery image/video selection and camera capture;
- file browser/file selection and opening of sent/received files;
- image/video bubble previews with at most three visible media tiles and a `+N` overflow tile;
- swipeable media viewer; videos do not autoplay;
- current-location sharing sends immediately after location acquisition;
- contact sharing reuses the existing Contacts selection UI and sends the selected contact immediately;
- contact bubbles show the available display name and phone number; tapping can add the contact to device contacts after confirmation;
- incoming image/video/file data is saved into the conversation attachment storage; location/contact blobs are not copied into the media/files storage tree;
- attachment storage overview/management and media export are available from Settings.

See [Attachments](attachments.md).

### Search and local AI

- normal local message search;
- optional semantic search;
- a shared on-device text-embedding model is downloaded/prepared only when a feature needs it;
- the downloaded model is integrity-checked before use;
- semantic indexing is built locally;
- search combines exact results with semantic results and falls back to exact results if semantic search is unavailable;
- search results navigate back to the matching Direct or Group message.

See [Message search](search.md).

### Message safety

- optional local message-safety analysis;
- structural checks for suspicious links/domains and high-risk request patterns;
- local embedding-based classification when the shared model is available;
- warning indicator in message presentation;
- details screen explaining detected reasons;
- block action from the safety details flow.

The analysis is local; it is not a cloud moderation service. See [Message safety](message-safety.md).

### Settings and diagnostics

- semantic-search and message-safety feature toggles with model/download/index state;
- attachment-storage entry and per-conversation attachment management;
- developer/network diagnostics;
- persisted developer error log with visible timestamps and a clear-log action.

See [Settings and diagnostics](settings.md).

### Network/operations UI

- configurable Control Plane directory loaded by `AppViewModel`;
- Settings Add field accepts a directory URL or one plane URL;
- multiple planes and health state;
- signed/verified node discovery;
- node failover/cooldown;
- current node and connection-count diagnostics;
- cooldown nodes shown with zero active connections.

### Offline/background

- WebSocket foreground delivery;
- recipient-selected mailbox delivery;
- Android FCM wake-up path;
- pending mailbox/envelope synchronization workers;
- conversation notifications/deep links.

## Server

### Control Plane

- signed Community Node registration/directory;
- node heartbeat/health expiry;
- Redis-backed presence routes;
- PostgreSQL-backed push registrations/wake-ups;
- Firebase Admin integration;
- Caddy reverse proxy and `/index` operator page;
- LAN/Public Windows launcher.

### Community Node

- client WebSocket gateway;
- signed presence-route registration/refresh;
- local delivery;
- cross-node federation;
- durable federation retry queue;
- recipient mailbox persistence/capabilities;
- encrypted blob upload/download used by message attachments;
- Control Plane directory parsing/caching/retry;
- continuous registration/heartbeat;
- Caddy edge and `/index` operator page;
- Windows and macOS/Linux launchers.

## Release/build automation

- PR validation on the `develop` integration path;
- change-aware release candidates on `release/**`;
- separate development and release Control Plane directory variables;
- signed/minified/resource-shrunk Android release APK;
- debug APK;
- affected-only Docker image rebuilds on release branch pushes;
- matching launcher-bundle rebuilds when image/config changes require them;
- full `v*` tag build of every image/package;
- public checksums/release metadata;
- one combined `sparrow-<version>-full.zip`;
- private R8 `mapping.txt` retention in Actions.

## Not currently advertised as working

- usable iOS client;
- multi-device identity synchronization;
- desktop client release;
- voice/video calling;
- a completed independent security audit.
