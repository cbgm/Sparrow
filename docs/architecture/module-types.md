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

- `:feature:identity` — local identity and private/public key persistence.
- `:feature:contacts` — contacts, invitations, verification, identity exchange and blocklist behavior.
- `:feature:contactimport` — importing/scanning identities and contact-related platform flows.
- `:feature:chats` — Direct and Group conversations, typed message parts, membership, delivery/read/typing and conversation UI.
- `:feature:attachments` — attachment source models, encrypted blob preparation/transfer/loading, cache, saved-copy storage and attachment management.
- `:feature:media` — gallery/camera/file selection, file browser, media rendering/opening/export.
- `:feature:messaging` — incoming/outgoing envelope processing and routing-ID resolution.
- `:feature:search` — exact and optional local semantic message search.
- `:feature:safety` — local message-risk analysis and warning/details UI.
- `:feature:transport` — Control Plane/node discovery, WebSocket client, presence registration, mailbox/push APIs, diagnostics.
- `:feature:onboarding` — first-run pages and permissions/phone/privacy setup.
- `:feature:settings` — user/developer/Control Plane settings, local-model feature toggles, attachment storage and developer error log.

### `:notification`

Notification orchestration and platform notification runtime hooks. Android provides the usable implementation today.

## Server modules

Shared: `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability`.

Control Plane applications: `:server:node-registry`, `:server:presence-directory`, `:server:push`.

Community Node applications: `:server:gateway`, `:server:federation`, `:server:mailbox`.

## Build/quality

- `build-logic/` — convention plugins and project build rules.
- `:quality:detekt-rules` — project-specific Detekt checks.
- `resources/` — shared Compose resources.
