# Current feature status

This page describes what is implemented in the current codebase. It is deliberately conservative: planned roadmap items are not listed as working features.

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
- conversation deletion/authorization-revocation behavior.

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
- re-invitation through a new active membership period.

There is no orphaned-group mode in the current architecture.

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
- attachments/file/media transfer;
- multi-device identity synchronization;
- desktop client release;
- voice/video calling;
- a completed independent security audit.
