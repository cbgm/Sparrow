# Sparrow documentation

Sparrow is an actively developed Kotlin Multiplatform secure-messaging project with an Android client and a
federated Kotlin/Docker server stack.

!!! warning "Platform status"
    Android is the usable client. iOS source sets and an Xcode host exist, but important platform/runtime
    functionality is still missing, so iOS is not currently a supported app target.

!!! info "Release/build source of truth"
    The current public server packager in this source tree builds one unified `dist/sparrow-server.zip`. The source archive used for this audit does not include `.github/workflows`, so exact CI trigger behavior is not asserted without those files.

## New here? Read these in order

1. [Introduction](getting-started/introduction.md)
2. [Current feature status](features/current-features.md)
3. [Project structure](getting-started/project-structure.md)
4. [Architecture overview](architecture/overview.md)
5. [Startup architecture](architecture/startup.md)
6. [Chats](features/chats.md) and [message transport flow](features/message-transport-flow.md)
7. [Attachments](features/attachments.md) and [Polls](features/polls.md)
8. [Identity recovery](features/identity-recovery.md), [Invitations](features/invitations.md) and [Group membership](features/group-membership.md)
9. [App lock](features/app-lock.md) and [Settings](features/settings.md)
10. [Server overview](server/overview.md)
11. [2026-10-08 release documentation audit](development/release-2026-10-08.md)

## Operator shortcuts

After a server package is running, you should not need to remember health URLs:

- **Control Plane:** open `/index` on the Control Plane address.
- **Community Node:** open `/index` on the Community Node address.

Those pages use relative links and therefore work in LAN and public deployments.

## Documentation sections

- **Why Sparrow?** — the federated design, Tor comparison, and where the security model differs from Signal/WhatsApp.
- **Getting Started** — prerequisites, first build, project structure, development flow.
- **Architecture** — Clean Architecture rules, module boundaries, Direct/Group separation.
- **Features** — current Android functionality, attachments/media, local message search, message safety and implementation classes.
- **Transport & API** — Control Plane discovery, node failover, WebSocket frames and packets.
- **Security** — identity, encryption, safety numbers, server trust and threat model.
- **Server & Operations** — Control Plane/Community Node, unified runtime manager, independent signed directory, Docker, Caddy and durable state.
- **Development** — local setup, extending the project, quality, testing, logging, contributing, releases.
- **Generated Reference** — current module/dependency reference plus a source-derived production class inventory.

## Documentation location

`README.md` at repository root is the only project Markdown documentation kept outside `docs/`. `server/secrets/placeholder.md` is not documentation; it only keeps the otherwise-empty secrets directory tracked by Git. Architecture, server, feature, security, API and development documentation is centralized here so there is one source of truth.

## Architecture source of truth

There are two types of architecture docs:

- `docs/generated/` is normally generated from Gradle project structure and dependencies.
- hand-written pages explain intent and runtime behavior.
- for this 2026-10-08 package, the generated reference was regenerated directly from `settings.gradle.kts`, module build files and Kotlin sources because Gradle 9.6.1 was unavailable in the isolated audit environment.

When the normal build environment is available, run:

```bash
./gradlew architectureReport
./gradlew verifyArchitectureReport
```

Do not manually edit generated files.
