# Frequently Asked Questions

## General

### What is SecureChat?

SecureChat is a Kotlin Multiplatform end-to-end encrypted messaging application focused on privacy, maintainability and clean architecture.

---

### Which platforms are supported?

The project is designed around Kotlin Multiplatform.

Currently Android is the primary platform.

The architecture allows additional platforms to reuse the same business logic.

---

### Is SecureChat open source?

The project architecture assumes open development and auditable cryptographic implementations.

Licensing depends on the repository configuration.

---

## Architecture

### Why are there so many modules?

The project intentionally separates responsibilities.

Benefits include

- better maintainability
- explicit dependencies
- faster builds
- easier testing

---

### Why use Clean Architecture?

Clean Architecture separates

- presentation
- business logic
- infrastructure

This keeps the application easier to maintain as it grows.

---

### Why Kotlin Multiplatform?

Business logic should be written once and reused across supported platforms.

Only platform-specific code remains platform dependent.

---

## Security

### Are messages encrypted?

Yes.

Messages are encrypted on the sender's device before being transmitted.

Only the intended recipient can decrypt them.

---

### Can the gateway read messages?

No.

The gateway only forwards encrypted packets.

It never receives plaintext.

---

### Are private keys uploaded?

No.

Private keys remain on the user's device.

Only public identity information is shared.

---

### Why are Safety Numbers needed?

Encryption alone cannot verify identity.

Safety Numbers allow two users to confirm they possess the expected public identity keys.

---

### What happens if a contact changes identity?

The previous verification becomes invalid.

The contact should be verified again before being trusted.

---

## Messaging

### Are messages stored locally?

Yes.

Messages are stored locally after successful decryption.

The gateway is not intended to become permanent message storage.

---

### Can messages be sent while offline?

Yes.

Messages are queued locally and transmitted after the connection has been restored.

---

### Are attachments supported?

Attachment support is planned.

Future attachments will use the same end-to-end encryption pipeline as text messages.

---

## Development

### Where should reusable code go?

Generally

```
Core
```

or

```
Shared
```

depending on whether the functionality is application-specific.

---

### Where should business logic go?

Business logic belongs inside

```
domain/
```

UseCases should remain independent of Android.

---

### Where should Compose code go?

Compose belongs inside

```
presentation/
```

Presentation should not implement business rules.

---

### Can ViewModels access Room?

No.

ViewModels communicate with repositories through UseCases.

Room belongs inside the Data layer.

---

## Build

### How do I build the project?

```bash
./gradlew build
```

---

### How do I run quality checks?

```bash
./gradlew quality
```

---

### How do I regenerate architecture documentation?

```bash
./gradlew architectureReport
```

---

### How do I install Git hooks?

```bash
./gradlew setup
```

---

### Should generated documentation be edited?

No.

Generated documentation should always be regenerated through Gradle.

---

## Documentation

### Which documentation is handwritten?

Examples include

```
Architecture

Security

Development

Features

API

Build
```

---

### Which documentation is generated?

Examples include

```
Module Pages

Dependency Matrix

Statistics

Mermaid Diagram
```

These files are regenerated automatically.

---

## Contributing

### Should formatting be performed manually?

No.

Execute

```bash
./gradlew qualityFix
```

instead.

---

### Should Detekt warnings be suppressed?

Only when absolutely necessary.

Fixing the underlying issue is strongly preferred.

---

### Should generated files be committed?

Yes.

Generated architecture documentation is considered part of the repository.

---

## Future

### Will desktop platforms be supported?

The architecture has been designed to support additional Kotlin Multiplatform targets in the future.

---

### Will group chats be supported?

Group chats are supported, including signed invitations, encrypted group messages, verification,
and owner-controlled member add/remove with epoch rotation. See
[Conversation, Messaging, and Delivery Flow](features/message-transport-flow.md#group-creation-and-per-member-activation).

---

### Will voice and video calls be supported?

These are long-term roadmap items.

They will build upon the existing identity, transport and encryption architecture.

---

# Summary

This FAQ answers the most common questions about SecureChat's architecture, security model, development workflow and build infrastructure.

When in doubt, consult the relevant handbook section for more detailed information.
