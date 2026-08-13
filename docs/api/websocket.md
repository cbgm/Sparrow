# WebSocket API

The client and `:server:gateway` exchange serialized sealed messages over text WebSocket frames.
Client models live in `:feature:transport/gateway`; matching compatibility models live in
`:server:protocol`.

## Connection

Gateway endpoints come from the verified node directory. Clients do not configure a static
WebSocket endpoint as their primary routing mechanism.

The external WebSocket path remains:

```text
/relay
```

This legacy path is intentionally preserved. The Kotlin/package refactor does not change the wire
endpoint.

Immediately after the Ktor WebSocket opens, the client sends `GatewayClientMessage.Register`.
The Kotlin property is `routingId`, while the JSON field remains `relayId` for compatibility:

```json
{
  "type": "register",
  "relayId": "scrouting1_..."
}
```

`GatewayServerMessage.Registered` uses the same compatibility field name. Until registration
succeeds, envelope, typing, and acknowledgement frames are rejected as not registered.

## Client-to-gateway messages

| Serial name | Kotlin type | Purpose |
|---|---|---|
| `register` | `GatewayClientMessage.Register` | Register the socket's canonical routing ID and optional signed route |
| `refresh_route` | `GatewayClientMessage.RefreshRoute` | Refresh signed route/aliases while connected |
| `send_envelope` | `GatewayClientMessage.SendEnvelope` | Submit an opaque `TransportEnvelope` |
| `send_federated_envelope` | `GatewayClientMessage.SendFederatedEnvelope` | Submit a federated envelope when that path is used |
| `typing_state` | `GatewayClientMessage.TypingState` | Send ephemeral typing state |
| `acknowledge_envelope` | `GatewayClientMessage.AcknowledgeEnvelope` | Confirm recipient-side processing |

## Gateway-to-client messages

| Serial name | Kotlin type | Purpose |
|---|---|---|
| `registered` | `GatewayServerMessage.Registered` | Confirm registration |
| `incoming_envelope` | `GatewayServerMessage.IncomingEnvelope` | Deliver an opaque transport envelope |
| `typing_state` | `GatewayServerMessage.TypingState` | Deliver ephemeral typing state |
| `envelope_accepted` | `GatewayServerMessage.EnvelopeAccepted` | Confirm gateway acceptance of an outgoing envelope |
| `error` | `GatewayServerMessage.Error` | Report a frame/registration/routing error |

The `@SerialName` discriminator values are protocol compatibility values. Renaming Kotlin classes
must not change them.

## TransportEnvelope

`TransportEnvelope` contains:

| Field | Meaning |
|---|---|
| `version` | Envelope format version |
| `envelopeId` | Transport-level idempotency and acknowledgement ID |
| `senderId` | Sender routing ID |
| `recipientId` | Recipient routing ID |
| `payload` | Opaque encoded SecureChat transport payload |
| `createdAtEpochMilliseconds` | Sender timestamp |

The gateway validates routing metadata, including that the sender is authorized for the registered
route/alias. It does not decode `payload` into SecureChat packets.

## Acceptance versus delivery

`GatewayServerMessage.EnvelopeAccepted` confirms only that the gateway accepted the outgoing
transport envelope. Recipient delivery is confirmed by SecureChat's delivery-receipt packet, and
read state by the read-receipt packet.

For an incoming envelope, the client acknowledges only after local processing succeeds. That
acknowledgement allows pending compatibility storage to remove its copy.

## Compatibility rules

When changing these models:

1. update both `:feature:transport` and `:server:protocol`;
2. preserve existing `@SerialName` discriminators unless performing an explicit protocol migration;
3. preserve the serialized registration field `relayId` until a separate wire migration is designed;
4. preserve `WS /relay` until a separate endpoint migration is designed;
5. add compatibility tests before changing any serialized field or discriminator.
