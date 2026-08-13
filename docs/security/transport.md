# Transport Security

## Overview

SecureChat separates transport from message security.

Transport is responsible only for delivering encrypted payloads between clients.

Message confidentiality is provided by end-to-end encryption and therefore does not depend on the transport layer.

This separation ensures that compromising the gateway does not reveal message contents.

---

# Design Goals

The transport layer has the following objectives.

- Reliable message delivery
- Stateless gateway forwarding
- No access to plaintext
- Platform-independent implementation
- Minimal protocol complexity
- Clear separation from cryptography

---

# Architecture

```
Sender

↓

Encrypt Message

↓

Transport Packet

↓

Gateway

↓

Transport Packet

↓

Decrypt Message

↓

Receiver
```

The gateway forwards packets exactly as received.

It never decrypts or interprets encrypted message contents.

---

# Responsibilities

The transport layer is responsible for

- establishing connections
- sending packets
- receiving packets
- reconnecting after disconnects
- delivery acknowledgements
- connection lifecycle management

The transport layer is **not** responsible for

- encryption
- identity verification
- message authenticity
- business logic

---

# WebSocket Connection

SecureChat uses a persistent WebSocket connection between the client and the gateway.

A persistent connection reduces latency and enables immediate delivery of incoming messages.

The connection manager automatically handles reconnection when necessary.

---

# Gateway

The gateway acts only as a forwarding service.

Responsibilities include

- accepting client connections
- registering connected identities
- routing encrypted packets
- forwarding packets to recipients

The gateway should not

- decrypt messages
- modify encrypted payloads
- inspect plaintext
- generate identities

---

# Transport Packet

Every transmitted packet contains transport metadata together with an encrypted payload.

Typical transport metadata includes

- sender identifier
- recipient identifier
- message identifier
- protocol version

The encrypted payload is treated as opaque binary data by the gateway.

---

# Encryption Boundary

Encryption occurs **before** a packet enters the transport layer.

```
Plaintext

↓

Encryption

↓

Encrypted Payload

↓

Transport

↓

Gateway
```

The transport layer never processes plaintext messages.

---

# Delivery Flow

The normal message flow is

```
Compose Message

↓

Encrypt

↓

Send Packet

↓

Gateway

↓

Receive Packet

↓

Decrypt

↓

Display Message
```

Every transport packet follows the same lifecycle.

---

# Connection Lifecycle

```
Disconnected

↓

Connecting

↓

Connected

↓

Disconnected
```

The connection manager is responsible for transitioning between these states.

Temporary network failures should not require user intervention.

---

# Reconnection

If the connection is interrupted, SecureChat attempts to reconnect automatically.

Typical causes include

- network changes
- temporary gateway outage
- device sleep
- application restart

The reconnection process should preserve pending outbound messages whenever possible.

---

# Outbox

Messages that cannot be transmitted immediately are placed into an outbound queue.

Typical lifecycle

```
Queued

↓

Sending

↓

Sent
```

If delivery fails

```
Queued

↓

Retry

↓

Sent
```

or

```
Queued

↓

Failed
```

depending on the error.

---

# Ordering

Transport attempts to preserve message order.

However, network conditions may cause packets to arrive out of order.

Message ordering should therefore be handled using message metadata rather than assuming arrival order.

---

# Duplicate Packets

The protocol should tolerate duplicate delivery.

Duplicate packets should be identified using message identifiers and ignored once processed.

This keeps retries safe.

---

# Error Handling

Transport errors should be categorized clearly.

Typical categories include

- connection failure
- timeout
- gateway unavailable
- malformed packet
- unsupported protocol version

Business logic should not depend on transport-specific exceptions.

---

# Authentication

Transport identifies connected clients using their public identity.

Authentication occurs independently of message encryption.

The gateway routes packets using identity information but cannot derive encryption keys from it.

---

# Transport Security

Transport security protects the communication channel.

Message security protects the message itself.

These are complementary concerns.

Even if transport encryption were compromised, end-to-end encrypted message payloads would remain unreadable without the appropriate private keys.

---

# Platform Independence

The transport implementation should remain platform-independent wherever practical.

Platform-specific networking code belongs inside platform source sets.

Protocol handling should remain inside common code.

---

# Testing

Transport should be tested independently of encryption.

Typical tests include

- connection establishment
- reconnection
- packet routing
- retry logic
- queue behaviour
- malformed packet handling

Cryptographic correctness should be tested separately.

---

# Summary

The transport layer provides reliable delivery of encrypted packets.

Its responsibility ends once packets have been transmitted or received.

Encryption, identity verification and business logic remain outside the transport layer, ensuring that each layer has a single, clearly defined responsibility.
