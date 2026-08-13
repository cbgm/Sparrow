# SecureChat Documentation

SecureChat is a Kotlin Multiplatform end-to-end encrypted messaging project built around modular architecture, explicit dependencies, automated quality checks, and generated architecture documentation.

## Start here

For a newly cloned repository:

```powershell
.\gradlew setup
```

For normal local formatting and verification:

```powershell
.\gradlew quality
```

For read-only verification used by Git hooks and CI:

```powershell
.\gradlew qualityCheck
```

## Handbook

- [Getting started](getting-started/introduction.md)
- [Architecture](architecture/overview.md)
- [Messaging boundary](architecture/messaging-boundary.md)
- [Conversation, messaging, and delivery flow](features/message-transport-flow.md)
- [Security](security/overview.md)
- [Features](features/chats.md)
- [Development](development/local-development.md)
- [Logging](development/logging.md)
- [Build infrastructure](build/index.md)
- [Protocol API](api/protocol.md) and [Gateway API](api/gateway.md)

## Generated architecture reference

The architecture plugin generates the following from the real Gradle project:

- [Generated overview](generated/index.md)
- [Module architecture](generated/architecture.md)
- [Module catalog](generated/modules.md)
- [Dependency matrix](generated/dependency-matrix.md)
- [Project statistics](generated/statistics.md)

It also generates one page per Gradle module and machine-readable JSON exports.

When modules, dependencies, source sets, or module contents change:

```powershell
.\gradlew architectureReport
```

Commit the updated files under `docs/generated/`.

Do not edit generated files manually.

## Messaging documentation

Use the messaging pages in this order:

1. [Messaging boundary](architecture/messaging-boundary.md) explains which module owns each responsibility.
2. [Conversation, messaging, and delivery flow](features/message-transport-flow.md) traces direct
   messages, group messages, receipts, typing, retries, gateway acknowledgements, and extension
   points using the production class names.
3. [Transport](features/transport.md) documents WebSocket, routing, and gateway mechanics.
4. [Protocol](api/protocol.md) documents packet types and serialization.

## Documentation workflow

Local Python, MkDocs, and Docker installations are not required. GitHub Actions builds and publishes the MkDocs site.

## Core principles

> Convention plugins configure infrastructure.  
> Modules declare their actual dependencies.  
> Architecture and quality rules are automated.  
> Generated documentation is derived from the project itself.
