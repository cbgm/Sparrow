# Gateway API

`:server:gateway` is the client-facing WebSocket edge of a community node. It accepts opaque
transport envelopes from connected clients, delivers locally connected recipients directly, and
hands remote/offline routing to federation. It never interprets SecureChat packet payloads.

## Public endpoints

| Endpoint | Purpose |
|---|---|
| `GET /health` | Liveness and active-connection count |
| `GET /v1/gateway/info` | Signed-route timing information used by current clients |
| `GET /v1/control-planes` | Control-plane URLs advertised by this node |
| `WS /v1/gateway` | Client WebSocket endpoint |

## Internal endpoints

The gateway also exposes authenticated node-to-node endpoints used by federation:

| Endpoint | Purpose |
|---|---|
| `POST /internal/v1/envelopes` | Deliver a federated envelope to a locally connected recipient |
| `POST /internal/v1/typing-events` | Deliver an ephemeral federated typing event |
| `GET /internal/v1/load` | Report active gateway connection load |
| `GET /internal/v1/routes/{routingId}` | Resolve an alias to its canonical routing ID |

These endpoints require the configured internal gateway token when internal authentication is
enabled.

## Client registration

After opening `WS /v1/gateway`, the client sends `GatewayClientMessage.Register`. The routing identifier is serialized as `routingId`:

```json
{
  "type": "register",
  "routingId": "scrouting1_..."
}
```

Current clients may also attach a connection ID, route generation, expiry, aliases, signing public
key, and signature. The gateway validates the signed route before publishing it to presence.

## Envelope acceptance

`GatewayClientMessage.SendEnvelope` carries a `TransportEnvelope`. A successful
`GatewayServerMessage.EnvelopeAccepted` means the gateway accepted the envelope into the routing
pipeline. It does **not** mean the recipient has persisted or read the message. Application-level
delivery and read receipts remain separate protocol packets.

## Security boundary

The gateway may observe routing metadata required to forward an envelope, but
`TransportEnvelope.payload` is opaque encrypted transport data. Packet meaning, conversation state,
identity policy, and message decryption stay on clients.

See [WebSocket API](websocket.md) for frame details and
[Transport](../features/transport.md) for the client implementation.
