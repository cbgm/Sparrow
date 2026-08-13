# Encryption

## Overview

Encryption is the core security mechanism of SecureChat.

Every message is encrypted on the sender's device before it is transmitted over the network.

For direct packets, only the intended recipient possesses the private X25519 key needed to open the
sealed transport payload. For group messages, every member of the current epoch possesses the same
group key.

The gateway never has access to plaintext.

---

# Design Goals

The encryption system has several primary goals.

- End-to-end encryption
- Strong authenticated encryption
- Client-side encryption only
- Clear separation from transport
- Platform-independent implementation
- Single cryptographic implementation

---

# Encryption Pipeline

Every outgoing message follows the same lifecycle.

```
Plaintext

↓

Serialize

↓

Encrypt

↓

Authenticated Ciphertext

↓

Transport Packet

↓

Gateway

↓

Transport Packet

↓

Decrypt

↓

Plaintext
```

The gateway only processes encrypted packets.

---

# Cryptographic Components

SecureChat separates encryption into independent responsibilities.

```
Identity Keys

↓

Key Agreement

↓

Message Key

↓

Authenticated Encryption

↓

Ciphertext
```

Each step has a clearly defined purpose.

---

# Authenticated Encryption

SecureChat uses authenticated encryption.

Group-message XChaCha20-Poly1305 provides

- confidentiality
- integrity
- ciphertext authenticity for holders of the shared epoch key

If encrypted data is modified during transport, decryption fails.

The application should never display partially decrypted data.

---

# Encryption Keys

Message encryption uses keys derived from the communicating identities.

Group encryption does not reuse identity keys directly for message encryption.

Instead

```
Identity Keys

↓

Recipient-specific sealed key distribution

↓

Random group epoch key

↓

Encrypt Message
```

This separates long-term identity from message encryption.

---

# Secure Group Messages

SecureChat group messages use a shared 256-bit key for each group epoch.

| Property | Implementation |
|---|---|
| Symmetric encryption | XChaCha20-Poly1305 through `SodiumGroupCrypto` |
| Nonce | New random 24-byte nonce for every encryption |
| Member attribution | Ed25519 signature from the individual sender |
| Key distribution | libsodium sealed box per recipient |
| Key at rest | AES-GCM under an AES-256 Android Keystore key |
| Epoch metadata | `GroupSecurityStateEntity` |
| Remote member key snapshot | `GroupMemberKeyEntity` |

AEAD alone cannot identify the sender because every current member knows the shared key.
`GroupSecurityManager.encryptMessage()` therefore signs the canonical header, nonce, and
ciphertext with the sender's Ed25519 private key. `GroupSecurityManager.decryptMessage()` selects
the expected public key from the authenticated contact and the stored epoch membership; it never
trusts a public key carried by the incoming message.

## Group creation and key distribution

`DefaultChatsRepository.createGroupConversation()` delegates to `GroupInvitationCoordinator`.
The coordinator can start with ordinary contacts whose secure identities are not known yet. It
creates a signed `GroupInvitePacket` with a random challenge for every selected contact. The
recipient verifies the owner identity and sees a pending group, but sends no identity until the user
accepts. Acceptance creates a signed `GroupJoinRequestPacket` carrying the invitee's public
encryption and signing keys.
`GroupInvitationManager` creates and verifies both signatures, while
`GroupInvitationCoordinator` binds the response to the persisted invitation, expected contact,
group, challenge, and expiry.

Discovered keys are stored as mutual but unverified. Users must still compare safety numbers to
protect the first contact from gateway-assisted identity substitution.

The first accepted contact causes `GroupSecurityManager.createOwnedGroup()` to generate epoch 1,
store the local key through `GroupKeyStorage`, and create its signed `GroupCreatedPacket`. Each
later accepted contact causes `GroupSecurityManager.rotateOwnedGroup()` to advance the epoch and
create one recipient-specific packet for every active member plus the joining contact. This keeps
pending contacts from blocking an already active group and prevents a new member from receiving an
older epoch key.

The raw epoch key is passed only in memory. `SodiumGroupCrypto.wrapGroupKey()` seals it to the
recipient's X25519 public key before the packet is encoded or enqueued. Consequently:

- `ProtocolOutboxEntity.encodedPacket` contains only a wrapped key;
- `GroupCreatedPacketHandler` must verify the owner's signature before accepting membership;
- only the intended recipient can unwrap `GroupCreatedPacket.wrappedGroupKey`;
- `DefaultOutboxProcessor` requires sealed outer transport for `GroupCreatedPacket`.

After persisting the key, `GroupCreatedPacketHandler` sends a signed
`GroupReadyAcknowledgementPacket` containing a SHA-256 confirmation derived from the group ID,
epoch, and recovered 256-bit key. The creator recomputes it with its local epoch key and does not
mark that member active until it has returned valid identity and key-possession proof. Pending
invitations do not block messages to members that are already active.

`GroupChatMessagePacket` may use plaintext outer transport because its content is already
XChaCha20-Poly1305 ciphertext authenticated by the group epoch key and an individual sender
signature. Such messages are stored as `GROUP_E2EE` so their security indicator describes the
inner group protection.

## Message protection

`GroupProtocolPayloadEncoder.encodeMessageAssociatedData()` binds these unencrypted fields to the
ciphertext:

- protocol version;
- group ID;
- epoch;
- message ID;
- sent timestamp.

`encodeMessageSignature()` additionally binds the random nonce and ciphertext. Changing any
bound value makes signature verification or AEAD authentication fail.

## Epochs and rotation

`GroupInvitationCoordinator.distributeGroupKeyToMember()` rotates an existing group when a newly
invited contact accepts. `GroupInvitationCoordinator.rotateAfterRemoval()` rotates before an
active contact is removed. Both paths call `GroupSecurityManager.rotateOwnedGroup()`, which:

1. requires a locally owned `GroupSecurityStateEntity`;
2. creates `nextEpoch = currentEpoch + 1`;
3. generates and stores a fresh 256-bit group key;
4. replaces the current `GroupMemberKeyEntity` snapshot;
5. signs a recipient-specific `GroupCreatedPacket` containing the complete membership and any
   identity-bound membership-change metadata;
6. wraps the new key separately for every remaining or joining recipient;
7. deletes locally stored keys and key rows from older epochs.

The removed contact is absent from the new snapshot and never receives the wrapped next-epoch key.
Incoming group messages must match `GroupSecurityStateEntity.currentEpoch` and a current
`GroupMemberKeyEntity`, so a removed sender's old-epoch packets are rejected. The owner also sends
a signed `GroupMemberRemovedPacket`, bound to the original invitation challenge. The removed device
verifies the owner and target identity, calls `GroupKeyStorage.deleteGroup()` and
`GroupSecurityDao.deleteGroup()`, then keeps the existing conversation as read-only. If removal
overtakes a welcome, the device can apply the owner-signed removal without an installed group state;
the invitation binding and removed signing identity still prevent a different member or group from
being targeted.

Every recipient-specific welcome ID binds `(groupId, invitationId, epoch)`. A member joining an
existing group may therefore install an epoch greater than 1 without previous local state, while a
welcome signed for an older invitation cannot activate a later re-invitation.

An active member leaves by signing `GroupLeaveRequestPacket` over the invitation ID/challenge,
group ID, installed epoch, member signing key, and request timestamp. The packet is sent with
pairwise `SEALED_BOX` outer encryption. After verifying that signature against the authenticated
contact and stored active invitation, the owner uses the same `rotateAfterRemoval()` path: the
leaving member is absent from the new key snapshot and receives no next-epoch key. The owner then
sends an owner-signed `GroupMemberRemovedPacket(reason = MEMBER_LEFT)` so the leaving device can
delete its old keys and retain only read-only history. The welcome sent to every remaining member
contains a signed `GroupMembershipChangePayload` with the leaving member's signing key.
`GroupCreatedPacketHandler` matches that key against the previous epoch before the key rows are
replaced, which produces “X left the group” without trusting a display name or an unbound reason.

---

# Plaintext

Plaintext exists only inside application memory.

It should never be

- written to logs
- transmitted directly
- exposed to the gateway
- persisted without protection

After encryption only ciphertext leaves the device.

---

# Ciphertext

Ciphertext is the encrypted representation of a message.

Ciphertext may safely travel through

- the gateway
- the Internet
- intermediate networks

Without the corresponding decryption key, ciphertext should reveal no useful information about the original message.

---

# Integrity Protection

Every encrypted message is protected against modification.

If any encrypted byte changes

```
Ciphertext

↓

Decrypt

↓

Authentication Failure
```

The message must be rejected.

Displaying corrupted plaintext is never acceptable.

---

# Replay Protection

The protocol should detect duplicate encrypted messages.

Message identifiers and protocol metadata allow implementations to ignore packets that have already been processed.

This prevents accidental duplicate delivery after retries.

---

# Encryption Boundary

The encryption boundary is intentionally simple.

Everything before encryption is trusted application code.

Everything after encryption is treated as untrusted transport data.

```
Application

↓

Encryption Boundary

↓

Transport

↓

Gateway
```

This separation simplifies reasoning about security.

---

# Error Handling

Decryption may fail for several reasons.

Examples include

- invalid ciphertext
- modified data
- unsupported protocol version
- missing keys
- corrupted packet

Failures should produce explicit error states rather than undefined behaviour.

---

# Message Authentication

Successful group AEAD decryption proves that

- the ciphertext has not been modified
- the sender possessed the current shared epoch key

The verified Ed25519 `senderSignature` separately proves which current member sent it.

Failed authentication should always result in message rejection.

---

# Cryptographic Isolation

Application code should never implement cryptographic algorithms directly.

Instead all cryptographic operations are delegated to the Core Crypto module.

Benefits include

- consistent implementations
- easier auditing
- centralized testing
- reduced duplication

---

# Randomness

Secure random values are required whenever cryptographic randomness is needed.

General-purpose pseudo-random generators must never be used for security-sensitive operations.

All randomness should originate from secure operating-system sources through the cryptographic library.

---

# Platform Independence

The encryption API is implemented in common code.

Platform-specific details remain hidden behind shared abstractions.

Business logic should never depend on Android cryptographic APIs directly.

---

# Testing

Encryption should be tested independently of transport.

Typical tests include

- successful encryption
- successful decryption
- invalid ciphertext
- modified ciphertext
- incorrect keys
- corrupted packets
- serialization compatibility

Every supported platform should produce identical cryptographic behaviour.

---

# Security Principles

The encryption implementation follows several principles.

- Private keys never leave the device.
- Plaintext never reaches the gateway.
- Every encrypted message is authenticated.
- Cryptographic code is centralized.
- Transport and encryption remain independent.

These principles reduce implementation complexity while improving auditability.

---

# Summary

Encryption is performed entirely on the communicating devices.

The gateway forwards only authenticated ciphertext and never gains access to plaintext.

By separating identity, key agreement, encryption and transport into independent layers, SecureChat provides a security model that is easier to understand, test and maintain.
