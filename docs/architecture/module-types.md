# Module types

## Application shell

### `:androidApp`
Android packaging, manifest, signing/minification and the smallest possible platform entry point.

### `:shared`
Shared Compose application shell, `AppViewModel`, common DI composition and KMP `BuildKonfig` configuration.

### `:navigation`
Typed navigation graphs and route wiring. Navigation does not contain conversation or server business logic.

### `:startup`
Startup/splash presentation state while application initialization is running.

## Core

- `:core` — cross-cutting primitives/utilities.
- `:core:crypto` — libsodium-backed crypto implementations.
- `:core:embedding` — shared local embedding model lifecycle and text-embedding runtime.
- `:core:protocol` — packet models/codecs, transport-independent outbox/sender contracts and attachment wire metadata.
- `:core:ui` — reusable UI/theme/navigation primitives.

## Data

- `:data:database` — Room entities, DAOs, database migrations and persistent protocol outbox.
- `:data:datastore` — shared key/value/settings persistence.

Room classes remain explicitly named `...Entity`; data representation DTOs use `...Dto`.

## Feature modules

- `:feature:identity` — local identity, identity exchange/trust, encrypted backup/restore, pending remote identity changes and approved reconnection state.
- `:feature:invite` — generic Direct/Group invitation persistence, state/result streams and invitation UI.
- `:feature:contacts` — contacts, phone/routing projections, blocking and device-contact integration; it does not own invitation lifecycle.
- `:feature:contactimport` — device/QR contact and identity import flows.
- `:feature:autoreply` — auto-reply definitions, activation and per-recipient claim/release state.
- `:feature:avatar` — target-aware avatar loading/cache plus profile-picture selection/cropping.
- `:feature:linkpreview` — URL preview cache/prefetch/presentation.
- `:feature:chats` — Direct/Group conversation and message semantics, message UI, delivery/read/typing and chat-specific outgoing/incoming processors.
- `:feature:conversationorchestration` — explicit cross-feature Invite/Identity/Membership/Chats/Transport workflows, outgoing packet policy/routing and recovery observers/workers.
- `:feature:membership` — Group membership handshakes, welcome/activation, current members/roles, epoch/group security and administration.
- `:feature:attachments` — attachment source models, encrypted blob preparation/transfer/loading, cache, saved-copy storage and attachment management.
- `:feature:media` — gallery/camera/file selection, file browser, media rendering/opening/export.
- `:feature:voice` — attachment-backed audio recording/playback and local transcription.
- `:feature:messaging` — generic durable protocol-outbox and incoming-envelope runners/processors.
- `:feature:search` — exact and optional local semantic message search.
- `:feature:safety` — local message-risk analysis and warning/details UI.
- `:feature:transport` — Control Plane/node discovery, WebSocket transport, presence/mailbox/push APIs and diagnostics.
- `:feature:onboarding` — first-run pages and permissions/phone/privacy setup.
- `:feature:settings` — user/developer/network settings, feature toggles, storage and developer error log.

### `:notification`

Notification orchestration and platform notification runtime hooks. Android provides the usable implementation today.

## Server modules

Shared: `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability`.

Control Plane applications: `:server:node-registry`, `:server:presence-directory`, `:server:push`.

Community Node applications: `:server:gateway`, `:server:federation`, `:server:mailbox`.

Additional server application: `:server:link-preview` for validated server-side URL metadata/image retrieval. `server/control-plane-directory` is a separate operator-only Python service, not a Gradle module and not part of the public server ZIP.

## Build/quality

- `build-logic/` — convention plugins and project build rules.
- `:quality:detekt-rules` — project-specific Detekt checks.
- `resources/` — shared Compose resources.
