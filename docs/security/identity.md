# Identity

## Overview

SecureChat uses public-key cryptography to establish the identity of every user.

Unlike traditional messaging systems, SecureChat identities are not based on usernames, passwords or phone numbers.

Instead, every user owns a cryptographic identity generated locally on their device.

The private portion of the identity never leaves the device.

---

# Goals

The identity system has several objectives.

- Unique cryptographic identity
- Local key generation
- Private keys never transmitted
- Public identities safely shareable
- Long-lived identity
- Platform-independent implementation

---

# Identity Components

A SecureChat identity consists of two independent key pairs.

```
Identity

├── Signing Key Pair (Ed25519)

└── Encryption Key Pair (X25519)
```

Each key pair serves a different purpose.

---

# Signing Keys

The signing key pair is used to authenticate the owner.

Responsibilities include

- proving identity
- signing identity information
- verifying authenticity

The signing private key must never leave the device.

The signing public key may be shared freely.

---

# Encryption Keys

The encryption key pair is used to establish encrypted communication.

Responsibilities include

- key agreement
- encrypted messaging
- session establishment

The encryption private key remains local.

The encryption public key is distributed together with the user's public identity.

---

# Identity Generation

Identity generation occurs entirely on the client.

```
Generate Randomness

↓

Create Signing Keys

↓

Create Encryption Keys

↓

Store Private Keys

↓

Publish Public Identity
```

No server participates in key generation.

---

# Public Identity

The public identity contains only information that is safe to distribute.

Typical fields include

- signing public key
- encryption public key
- optional display name
- optional phone number

Private keys are never included.

---

# Private Identity

Private identity material remains on the device.

It includes

- signing private key
- encryption private key

Private keys are stored using the platform's secure storage mechanisms whenever available.

---

# Identity Lifetime

A SecureChat identity is intended to be long-lived.

Changing identity creates a completely new cryptographic identity.

Other users must therefore verify the new identity before trusting it.

---

# Sharing Identity

Users exchange public identities.

Sharing methods may include

- QR codes
- secure links
- files
- direct transfer

Only public information is exchanged.

---

# Importing an Identity

After receiving another user's public identity, SecureChat

1. validates the payload
2. imports the public keys
3. creates or updates the corresponding contact
4. marks the identity as unverified

Verification is performed separately.

---

# Trust

Receiving a public identity does not automatically establish trust.

The imported identity should be verified using the safety number mechanism.

Until verification has been completed, encrypted communication may still occur, but the identity should be considered unverified.

---

# Identity Changes

If another user's public identity changes unexpectedly, SecureChat should treat this as a significant security event.

Possible reasons include

- legitimate reinstallation
- new device
- identity reset
- malicious impersonation attempt

Previously verified identities should require verification again after such a change.

---

# Identity Verification

Identity verification is performed through safety numbers.

The safety number is derived from both users' public identity keys.

Matching safety numbers confirm that both users possess the expected identities.

---

# Phone Numbers

Phone numbers are optional metadata.

They improve contact discovery but do not define identity.

The cryptographic identity remains the authoritative identifier.

Changing a phone number does not require generating a new identity.

---

# Display Names

Display names are user-facing labels only.

They are not security identifiers.

Users should never rely solely on display names when determining authenticity.

---

# Storage

Identity information is divided into two categories.

Public identity

- may be synchronized
- may be shared
- may be stored in the contact database

Private identity

- remains local
- is protected
- is never transmitted

---

# Security Considerations

The identity system is designed so that

- servers cannot generate identities
- gateways cannot impersonate users
- public identities can be distributed safely
- private keys remain confidential

Compromise of the gateway does not reveal private identity material.

---

# Summary

Every SecureChat user owns a cryptographic identity consisting of independent signing and encryption key pairs.

Public identity information is shared with other users.

Private keys never leave the device.

Trust is established through safety-number verification rather than by relying on names or phone numbers alone.
