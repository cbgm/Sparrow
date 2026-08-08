# WebSocket API

The client and relay exchange serialized sealed messages over text WebSocket frames. Client models
live under `:feature:transport`; matching server models live under `:relay`.

## Connection

The WebSocket URL comes from the signed node directory returned by the configured registry.
There is no statically configured client relay URL. Immediately after the Ktor WebSocket opens,
the client sends `RelayClientMessage.Register`.

```json
{
  "type": "register",
  "relayId": "..."
}
```

The exact discriminator name is configured by each module's relay `Json`; use the Kotlin model and
`createRelayJson()` as the source of truth rather than hand-serializing frames.

The relay responds with `RelayServerMessage.Registered`. Until registration succeeds, envelope,
typing, and acknowledgement messages are rejected with `NOT_REGISTERED`.

## Client-to-relay messages

| Serial name | Kotlin type | Purpose |
|---|---|---|
| `register` | `RelayClientMessage.Register` | Associate the socket with one relay ID |
| `send_envelope` | `RelayClientMessage.SendEnvelope` | Ask the relay to accept an opaque envelope |
| `typing_state` | `RelayClientMessage.TypingState` | Forward ephemeral typing state |
| `acknowledge_envelope` | `RelayClientMessage.AcknowledgeEnvelope` | Confirm recipient-side processing |

## Relay-to-client messages

| Serial name | Kotlin type | Purpose |
|---|---|---|
| `registered` | `RelayServerMessage.Registered` | Confirm registration |
| `incoming_envelope` | `RelayServerMessage.IncomingEnvelope` | Deliver a pending envelope |
| `typing_state` | `RelayServerMessage.TypingState` | Forward sender typing state |
| `envelope_accepted` | `RelayServerMessage.EnvelopeAccepted` | Confirm relay storage/acceptance |
| `error` | `RelayServerMessage.Error` | Report protocol or routing error |

## Envelope

`RelayEnvelope` version 1 contains:

| Field | Meaning |
|---|---|
| `envelopeId` | Relay-level idempotency and acknowledgement ID |
| `senderId` | Sender relay address |
| `recipientId` | Recipient relay address |
| `payload` | Opaque encoded SecureChat transport payload |
| `createdAtEpochMilliseconds` | Ordering timestamp used by the pending store |

The relay validates that `senderId` matches the registered connection. It does not decode
`payload`.

## Acceptance versus delivery

After `DefaultRelayEnvelopeRouter.accept()` stores an envelope, `RelayWebSocketHandler` sends
`EnvelopeAccepted` to the sender. This means the sender may move to `MessageDeliveryStatus.SENT`.

The relay then sends `IncomingEnvelope` to a connected recipient. After the recipient finishes
local handling, it sends `AcknowledgeEnvelope`; only then does the relay remove the pending copy.

End-user `DELIVERED` is represented by a separate SecureChat `DeliveryReceiptPacket` travelling
inside another envelope.

## Errors

`RelayWebSocketHandler` currently emits these codes:

| Code | Cause |
|---|---|
| `UNSUPPORTED_FRAME` | Frame is not text |
| `INVALID_MESSAGE` | Relay message cannot be decoded |
| `NOT_REGISTERED` | Operation sent before registration |
| `ALREADY_REGISTERED` | Second registration on one socket |
| `SENDER_MISMATCH` | Envelope sender differs from registered relay ID |
| `ENVELOPE_REJECTED` | Pending store rejected the envelope |

`DefaultWebSocketTransportClient` logs `RelayServerMessage.Error` without unconditionally closing
the socket. A waiting envelope acceptance fails when its configured timeout expires.

## Typing

Typing state is only forwarded when the recipient is currently connected. It is not stored and has
no acknowledgement.

## Compatibility

Client and server keep separate copies of relay message models. Any relay schema change must update
both copies and their JSON configuration together. Add integration coverage before deploying mixed
versions.
