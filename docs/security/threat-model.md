# Threat Model

## Overview

A threat model defines what SecureChat is designed to protect against and, equally importantly, what it does **not** attempt to protect against.

Understanding these assumptions is essential when evaluating the security of the application.

No messaging application can defend against every possible attack.

Instead, SecureChat focuses on realistic threats that can be mitigated through cryptography and sound software architecture.

---

# Security Objectives

SecureChat is designed to provide

- Confidentiality
- Integrity
- Authenticity
- Identity verification
- Secure key management
- End-to-end encrypted communication

---

# Trust Model

SecureChat intentionally trusts very few components.

```
User Device

✓ Trusted

↓

Application

✓ Trusted

↓

Cryptographic Library

✓ Trusted

↓

Gateway

✗ Untrusted

↓

Internet

✗ Untrusted
```

Only the communicating devices are considered trusted.

Everything between them is treated as hostile.

---

# Protected Assets

SecureChat protects

- private identity keys
- message contents
- contact identities
- session keys
- encrypted attachments
- verification state

Loss of any of these assets may compromise user privacy.

---

# Threats Addressed

## Passive Network Monitoring

An attacker observes network traffic.

```
Attacker

↓

Network

↓

Encrypted Traffic
```

Result

Message contents remain confidential.

---

## Malicious Wi-Fi Networks

An attacker controls the local network.

The attacker may

- inspect packets
- delay packets
- drop packets

The attacker cannot read encrypted message contents.

---

## Compromised Gateway

The gateway service is assumed to be untrusted.

A malicious gateway may

- observe connections
- delay messages
- refuse delivery
- replay packets

The gateway cannot

- decrypt messages
- generate valid signatures
- recover private keys

---

## Message Modification

An attacker modifies encrypted packets during transport.

```
Ciphertext

↓

Modified

↓

Authentication Failure
```

Modified packets are rejected.

---

## Identity Substitution

An attacker attempts to replace another user's public identity.

Protection

- Safety Numbers
- Identity Verification

Group invitation bootstrap proves that the same endpoint controls the private keys corresponding
to the public identity in `GroupInvitePacket` or `GroupJoinRequestPacket`. It does not prove the
real-world identity of a first-time contact when the routing address itself is the only trusted
addressing information.

Users should verify important contacts before trusting them. Automatically discovered group
identities are stored as mutual but unverified until safety numbers are compared.

---

## Replay Attacks

An attacker resends previously transmitted packets.

Protection

- Message identifiers
- Duplicate detection
- Persisted invitation IDs and challenges
- Invitation expiry checked by the group owner

---

## Premature Group-Key Distribution

An attacker or incomplete invitation flow attempts to make a group usable before all intended
members have authenticated keys.

Protection

- `GroupInvitationEntity` persists readiness for each selected contact
- the invitee must explicitly accept before sending `GroupJoinRequestPacket`
- `GroupInvitationCoordinator` distributes a key only to contacts that explicitly accepted
- the creator treats a member as active only after a signed `GroupReadyAcknowledgementPacket`
- epoch 1 is not generated before activation
- creator messages remain local queued rows until at least one member confirms key installation
- adding or removing an active member rotates to a fresh epoch and complete member-key snapshot
- removed members receive no wrapped next-epoch key
- `GroupMemberRemovedPacket` is owner-signed and bound to the original invitation challenge

Previously processed messages should not be accepted again.

---

## Unauthorized Message Reading

An attacker obtains encrypted packets.

Without the appropriate private keys the attacker cannot recover plaintext.

---

## Group Packet Forgery

Every current member knows the shared group epoch key, so group AEAD by itself cannot attribute a
message to one member. SecureChat additionally requires an Ed25519 signature and verifies it
against the sender's `GroupMemberKeyEntity` for the exact epoch.

A network attacker, gateway, removed non-member, or different contact therefore cannot forge a
current member's group message without that member's signing private key.

---

# Threats Not Addressed

SecureChat does **not** protect against every possible threat.

---

## Compromised Device

If malware gains full control of a user's device

- plaintext may be accessible
- private keys may be exposed
- screenshots may be captured

Application-level encryption cannot defend against a fully compromised endpoint.

For a group, compromise of any current member exposes that epoch's shared key and therefore the
content encrypted under that key. It does not provide the other members' Ed25519 private keys, so
the attacker still cannot impersonate a different member without also compromising that key.

---

## Shared-Key Limits

The current group design does not provide Signal-style pairwise or sender-key ratcheting, automatic
post-compromise security, or cryptographic deniability. Epoch rotation infrastructure exists, but
membership-change/rekey protocol support is the next required feature. Until that is implemented,
groups are static after creation.

When rekey support is added, removal prevents a removed member from reading new epochs only after
the new key has been distributed and activated. No protocol can make a member forget plaintext or
keys it already received.

---

## Malicious Operating System

If the operating system itself is compromised, SecureChat cannot guarantee confidentiality.

The application depends on the integrity of the host operating system.

---

## Physical Device Access

An attacker with prolonged physical access to an unlocked device may be able to access

- decrypted messages
- active sessions
- cached information

Device security remains the user's responsibility.

---

## Social Engineering

SecureChat cannot prevent users from voluntarily sharing

- Safety Numbers
- Screenshots
- Plaintext
- Verification codes

Users remain responsible for verifying identities through trusted channels.

---

## Traffic Analysis

Even though message contents are encrypted, some metadata remains observable.

Examples include

- connection timing
- online status
- packet frequency
- approximate communication patterns

SecureChat minimizes metadata but does not eliminate traffic analysis completely.

---

# Assumptions

SecureChat assumes

- cryptographic primitives remain secure
- operating-system secure storage functions correctly
- random-number generation is secure
- users verify important contacts
- private keys remain private

If these assumptions fail, security guarantees may no longer hold.

---

# Defense in Depth

SecureChat uses multiple independent security layers.

```
Identity

↓

Authentication

↓

Encryption

↓

Transport

↓

Secure Storage
```

Breaking one layer should not automatically compromise the others.

---

# Failure Strategy

Whenever SecureChat cannot determine that an operation is secure, it should fail safely.

Examples

- Reject invalid ciphertext
- Reject invalid signatures
- Reject malformed packets
- Reject unsupported protocol versions

Failing safely is preferable to accepting uncertain data.

---

# Security Reviews

Changes involving

- cryptography
- identity management
- protocol serialization
- key storage
- transport security

should receive additional review before merging.

Small, focused security changes are easier to audit than large mixed commits.

---

# Future Threats

The threat model should evolve alongside the application.

New features such as

- multi-device support
- encrypted backups
- voice/video calls
- desktop clients

introduce additional attack surfaces and should be accompanied by corresponding threat-model updates.

---

# Summary

SecureChat assumes that the network and gateway infrastructure are untrusted.

Security is achieved by ensuring that only the communicating devices possess the cryptographic material required to authenticate identities and decrypt messages.

The application cannot protect against fully compromised endpoint devices, but it is designed to remain secure even when the transport infrastructure is completely hostile.
