# Design Decisions

## Overview

This document records the major architectural decisions made during the development of SecureChat.

Unlike implementation documentation, these decisions explain **why** the project is designed the way it is.

Recording design decisions helps future contributors understand the reasoning behind the architecture and reduces the likelihood of revisiting previously resolved discussions.

---

# ADR-001

## Kotlin Multiplatform

### Decision

Use Kotlin Multiplatform as the primary development platform.

### Rationale

Business logic should be reusable across multiple platforms.

Benefits include

- shared domain logic
- shared cryptography
- shared protocol
- shared transport
- reduced duplication

### Consequences

Platform-specific code remains isolated inside platform source sets.

---

# ADR-002

## Modular Architecture

### Decision

Split the project into multiple Gradle modules.

### Rationale

Large monolithic modules become increasingly difficult to maintain.

Benefits

- explicit dependencies
- faster compilation
- improved ownership
- easier testing

### Consequences

Architecture validation becomes important.

---

# ADR-003

## Clean Architecture

### Decision

Every feature follows

```
Presentation

↓

Domain

↓

Data
```

### Rationale

Separating business rules from implementation details improves

- maintainability
- testing
- scalability

### Consequences

Additional abstractions are justified by improved long-term flexibility.

---

# ADR-004

## Convention Plugins

### Decision

Use convention plugins instead of duplicated Gradle configuration.

### Rationale

Large projects often repeat identical Gradle configuration.

Convention plugins centralize

- Kotlin
- Android
- Compose
- testing

### Consequences

Module build files remain small and consistent.

---

# ADR-005

## Version Catalog

### Decision

Manage every dependency through the Gradle Version Catalog.

### Rationale

A single source of truth simplifies dependency management.

### Consequences

No module should hardcode dependency versions.

---

# ADR-006

## Automatic Architecture Documentation

### Decision

Generate architecture documentation automatically.

### Rationale

Manual diagrams quickly become outdated.

The Gradle project already contains the required information.

### Consequences

Generated documentation is always synchronized with the repository.

---

# ADR-007

## Generated Mermaid Diagrams

### Decision

Generate Mermaid diagrams directly from the discovered module graph.

### Rationale

Avoid maintaining architecture diagrams manually.

### Consequences

Architecture diagrams become deterministic.

---

# ADR-008

## End-to-End Encryption

### Decision

Encrypt every message before transport.

### Rationale

Transport infrastructure must never have access to plaintext.

### Consequences

The gateway becomes an untrusted forwarding service.

---

# ADR-009

## Cryptographic Identity

### Decision

Every user owns a locally generated cryptographic identity.

### Rationale

Identity should not depend on usernames, passwords or servers.

### Consequences

Private keys never leave the device.

---

# ADR-010

## Separate Signing and Encryption Keys

### Decision

Maintain separate key pairs.

```
Ed25519

↓

Signing

X25519

↓

Encryption
```

### Rationale

Separate responsibilities improve security and follow established cryptographic practice.

---

# ADR-011

## Safety Numbers

### Decision

Verify identities using Safety Numbers.

### Rationale

Encryption alone does not establish trust.

Safety Numbers detect unexpected identity changes.

### Consequences

Users can verify important contacts through independent channels.

---

# ADR-012

## Stateless Gateway

### Decision

Keep the gateway intentionally simple.

### Responsibilities

- routing
- forwarding
- connection management

### Rationale

Business logic should remain on the clients.

### Consequences

The gateway cannot decrypt messages.

---

# ADR-013

## Explicit Dependencies

### Decision

Runtime dependencies remain explicit inside module build files.

### Rationale

Convention plugins should configure infrastructure rather than hiding runtime behaviour.

### Consequences

Dependency graphs remain understandable.

---

# ADR-014

## Configuration Cache

### Decision

Design custom Gradle tasks for Configuration Cache compatibility.

### Rationale

Large modular builds benefit significantly from reduced configuration time.

### Consequences

Tasks use Providers and declared inputs/outputs.

---

# ADR-015

## Repository-Managed Git Hooks

### Decision

Track Git hooks inside the repository.

### Rationale

Every developer should execute identical quality checks.

### Consequences

Hooks are installed through

```bash
./gradlew setup
```

---

# ADR-016

## Automated Quality Pipeline

### Decision

Centralize verification into

```
quality
```

### Rationale

Developers should execute one command rather than many.

### Consequences

Formatting, architecture and static analysis remain synchronized.

---

# ADR-017

## Machine-Readable Reports

### Decision

Generate JSON together with Markdown.

### Rationale

Future tooling should reuse the architecture model.

### Consequences

Dashboards and IDE integrations can consume generated metadata directly.

---

# ADR-018

## CommonMain First

### Decision

Business logic belongs inside

```
commonMain
```

whenever practical.

### Rationale

Platform independence remains a long-term project goal.

### Consequences

Android-specific code stays isolated.

---

# ADR-019

## Living Documentation

### Decision

Treat documentation as part of the source tree.

### Rationale

Architecture documentation should evolve together with implementation.

### Consequences

Generated reports are committed alongside architectural changes.

---

# ADR-020

## Single Source of Truth

### Decision

Avoid duplicate representations of project structure.

### Rationale

Every duplicate eventually diverges.

The Gradle project is the authoritative source.

### Consequences

Every generated artifact derives from the same architecture model.

---

# ADR-021

## Project-Owned Multiplatform Logging

### Decision

Expose `SecureChatLogger` from `:core` and back it with Kermit for shared and application code.
Keep JVM server services on SLF4J with Logback.

### Rationale

Kermit provides platform-specific output for Kotlin Multiplatform targets. A project-owned facade
keeps feature modules independent of the logging vendor and provides one place for future crash
reporting or telemetry integration.

The server applications run in a JVM environment where SLF4J is the standard boundary and
Logback is configured.

### Consequences

- feature modules use `SecureChatLog.withTag(...)`;
- Kermit remains an implementation dependency of `:core`;
- server logs participate in the existing server logging pipeline;
- Detekt rejects raw console and stack-trace printing;
- log messages must follow the project's privacy policy.

See the [Logging guide](development/logging.md).

---

# Future ADRs

Every significant architectural decision should be recorded here.

Typical candidates include

- protocol evolution
- multi-device support
- encrypted backups
- group messaging
- desktop applications
- post-quantum cryptography

Recording decisions makes future maintenance significantly easier.

---

# Summary

These Architectural Decision Records capture the reasoning behind SecureChat's most important design choices.

Understanding **why** the project is structured as it is is just as valuable as understanding **how** it is implemented.
