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
2. [What makes Sparrow different?](why-sparrow.md)
3. [Installation](getting-started/installation.md)
4. [First build](getting-started/first-build.md)
5. [Using the Android app](getting-started/using-app.md)
6. [Local development on Windows/macOS](development/local-development.md)
7. [Current feature status](features/current-features.md)
8. [Runtime orchestration and protocol flows](architecture/runtime-flows.md)
9. [Identity backup, recovery and reconnection](features/identity-recovery.md)
10. [Invitations](features/invitations.md)
11. [Group membership](features/group-membership.md)
12. [Attachments](features/attachments.md)
9. [Message search](features/search.md)
10. [Message safety](features/message-safety.md)
11. [Settings and diagnostics](features/settings.md)
12. [Server overview](server/overview.md)
13. [Architecture overview](architecture/overview.md)
14. [Conversation, messaging and delivery flow](features/message-transport-flow.md)
15. [Security overview](security/overview.md)

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

- `docs/generated/` is generated from Gradle project structure and dependencies.
- hand-written pages explain intent and runtime behavior.

When module dependencies change, run:

```bash
./gradlew architectureReport
./gradlew verifyArchitectureReport
```

Do not manually edit generated files.
