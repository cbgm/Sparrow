# Security Overview

## Introduction

Security is the primary design goal of SecureChat.

Every architectural decision is evaluated from a security perspective before convenience or performance.

Unlike traditional messaging applications, SecureChat assumes that transport infrastructure cannot be trusted.

The gateway forwards encrypted data but must never be able to read message contents.

---

# Security Goals

SecureChat is designed around the following goals.

- End-to-end encryption
- Strong identity verification
- Forward secrecy where applicable
- Platform-independent cryptography
- Explicit trust model
- Minimal metadata exposure
- Open and auditable implementation

---

# Threat Model

SecureChat protects against

- passive network observers
- malicious Wi-Fi networks
- compromised gateway services
- message interception
- message modification
- identity spoofing
- unauthorized message reading

SecureChat does **not** attempt to protect against a fully compromised endpoint device.

If an attacker controls the user's device, application-level encryption cannot guarantee confidentiality.

---

# Security Architecture

```
User A

↓

Identity Keys

↓

Session Keys

↓

Encrypted Message

↓

Gateway

↓

Encrypted Message

↓

Session Keys

↓

Identity Keys

↓

User B
```

The gateway only transports encrypted payloads.

It never receives plaintext.

---

# Security Layers

SecureChat separates security into several independent layers.

```
Identity

↓

Authentication

↓

Transport

↓

Encryption

↓

Storage
```

Each layer has a single responsibility.

Compromise of one layer should not automatically compromise the others.

---

# Identity

Every SecureChat user owns a cryptographic identity.

The identity is generated locally.

Private keys never leave the user's device.

The public identity can be shared safely.

Identity generation is described in detail in **identity.md**.

---

# Authentication

Authentication is based on public-key cryptography.

SecureChat authenticates users through their cryptographic identities rather than usernames or passwords.

This prevents identity spoofing during encrypted communication.

---

# Encryption

Messages are encrypted before leaving the device.

Encryption occurs entirely on the client.

The gateway never performs encryption or decryption.

Only the communicating devices possess the keys required to decrypt message contents.

Direct packets use recipient sealed-box transport. Group messages use one XChaCha20-Poly1305 key
per epoch plus an Ed25519 signature from the individual sender. See
[Encryption](encryption.md#secure-group-messages) for the concrete classes and trust boundaries.

A group can be created from ordinary contacts even when their secure identities are not stored
yet. The recipient explicitly accepts a signed `GroupInvitePacket`; its signed
`GroupJoinRequestPacket` then proves possession of the exchanged public keys. After welcome-key
installation, `GroupReadyAcknowledgementPacket` prevents the creator from releasing queued content
too early. Automatically discovered identities remain unverified until users compare safety
numbers.

---

# Transport

Transport security is independent from message security.

Even if the transport layer is intercepted, encrypted payloads remain unreadable without the appropriate cryptographic keys.

---

# Gateway

The gateway service has a deliberately limited role.

Responsibilities include

- client registration
- message forwarding
- connection management

The gateway

- cannot decrypt messages
- cannot generate identities
- cannot verify safety numbers
- cannot modify encrypted content without detection

---

# Local Storage

Sensitive information is stored only when necessary.

Examples include

- encrypted private keys
- encrypted message database
- cached public identities

Private key material is protected using platform security mechanisms.

---

# Safety Numbers

SecureChat allows users to verify one another through safety numbers.

Verification ensures that both parties possess the expected public identity keys.

Once verified, the application can detect unexpected identity changes.

---

# Metadata

While message contents are encrypted, some metadata remains unavoidable.

Examples include

- connection times
- approximate message timing
- online status
- network addresses

SecureChat minimizes metadata where practical but does not claim to eliminate it completely.

---

# Cryptographic Libraries

Cryptographic operations are centralized inside the Core Crypto module.

Application code should never implement cryptographic algorithms directly.

This approach

- reduces duplication
- simplifies auditing
- prevents inconsistent implementations

---

# Secure Defaults

SecureChat prefers secure defaults over optional security.

Examples include

- encryption enabled automatically whenever possible
- verified identities retained
- plaintext avoided when encrypted communication is available

Users should not need deep cryptographic knowledge to communicate securely.

---

# Build-Time Security

The project includes automated checks that help maintain security.

Examples include

- architecture validation
- custom Detekt rules
- forbidden cryptographic APIs
- commonMain platform restrictions

These checks reduce the likelihood of introducing accidental security regressions.

---

# Security Reviews

Every security-sensitive change should be reviewed carefully.

Particular attention should be given to

- cryptographic code
- protocol changes
- identity management
- message serialization
- key storage

Security-related changes should remain as small and focused as possible.

---

# Responsible Design

SecureChat intentionally separates

- security decisions
- business logic
- presentation
- infrastructure

This separation makes the implementation easier to understand and audit.

Security should never depend on UI behaviour.

---

# Summary

Security is not a single feature within SecureChat.

It is a property of the entire architecture.

Identity, encryption, transport and storage are designed to work together so that sensitive information remains protected throughout its lifetime.

The following documents describe each security component in detail.

- Identity
- Transport
- Encryption
- Safety Numbers
- Threat Model
