# Transport

`:feature:transport` owns node discovery, routing IDs, gateway/WebSocket mechanics, push/mailbox HTTP
adapters, and the `OutgoingWireSender` implementation. It does not own conversations, contacts,
message-delivery rules, crypto policy, or persistent outbox orchestration.

For the complete direct/group application flow, see
[Conversation, Messaging, and Delivery Flow](message-transport-flow.md).

## Boundary

| Transport owns | Owned elsewhere |
|---|---|
| Ktor `HttpClient` creation | Packet definitions in `:core:protocol` |
| Gateway JSON and frame models | Encryption and payload codec in `:core:crypto` |
| Local routing-ID generation | Contact/group routing resolution in `:feature:messaging` |
| Signed node discovery and failover | Persistent outbox in `:data:database` |
| WebSocket registration and frames | Send/receive orchestration in `:feature:messaging` |
| Push/mailbox network adapters | Mailbox orchestration in `:feature:messaging` |
| `OutgoingWireSender` implementation | Message/receipt behavior in `:feature:chats` |

`TransportEnvelope.payload` is opaque to this module.

## Package map

```text
feature/transport/.../feature/transport/
├── config/
│   └── TransportConfig.kt
├── connection/
│   ├── TransportConnectionManager.kt
│   ├── DefaultTransportConnectionManager.kt
│   ├── TransportConnectionState.kt
│   └── TransportDiagnosticsState.kt
├── controlplane/
├── discovery/
├── gateway/
│   ├── codec/
│   │   └── GatewayJson.kt
│   └── model/
│       ├── GatewayClientMessage.kt
│       ├── GatewayServerMessage.kt
│       ├── GatewayTypingEvent.kt
│       ├── TransportEnvelope.kt
│       ├── FederatedEnvelope.kt
│       └── ClientRouteRegistration.kt
├── mailbox/
├── presence/
├── push/
│   └── inbox/
├── routing/
│   ├── LocalRoutingIdProvider.kt
│   ├── DefaultLocalRoutingIdProvider.kt
│   ├── LocalBootstrapRoutingIdProvider.kt
│   ├── DefaultLocalBootstrapRoutingIdProvider.kt
│   ├── RoutingIdGenerator.kt
│   └── Sha256RoutingIdGenerator.kt
├── sender/
├── websocket/
└── di/
```

## Routing IDs

`RoutingIdGenerator` derives canonical routing IDs from signing identities. Routing terminology is
consistent across the Kotlin API, gateway wire model, local Room schema, and server-side storage.

The canonical client WebSocket endpoint is `WS /v1/gateway`. Gateway timing information is available
through `GET /v1/gateway/info`.

## Connection lifecycle

`DefaultTransportConnectionManager` implements `TransportConnectionManager`. It:

1. obtains the local routing ID;
2. synchronizes/validates control-plane and node-directory information;
3. selects a compatible gateway endpoint;
4. skips nodes in failed-node cooldown;
5. connects `WebSocketTransportClient`;
6. waits for registered/failed state;
7. reconnects through another verified node after failure or closure.

A static WebSocket URL is not the normal source of node selection. Verified control-plane/node
information supplies compatible gateway endpoints.

## Registration

After the socket opens, `DefaultWebSocketTransportClient` sends
`GatewayClientMessage.Register(routingId = ...)`. The client becomes connected only after a matching
`GatewayServerMessage.Registered` is received.

Signed route registration also carries the connection generation, expiry, aliases, signing public
key, and proof needed by the presence system.

## Outgoing envelopes

`WebSocketOutgoingWireSender` implements the transport-independent `OutgoingWireSender` port from
`:core:protocol`.

It validates its inputs, obtains the local routing ID, creates a versioned `TransportEnvelope`, and
calls `sendEnvelopeAndAwaitAcceptance()` using `TransportConfig.acknowledgementTimeoutMilliseconds`.

`GatewayServerMessage.EnvelopeAccepted` completes the transport acceptance wait. It does not imply
recipient delivery or read state.

## Incoming envelopes

`DefaultWebSocketTransportClient.incomingEnvelopes` emits opaque `TransportEnvelope` objects.
`WebSocketIncomingEnvelopeGateway` in `:feature:messaging/data/routing` maps them to
`IncomingTransportEnvelope`; `DefaultIncomingEnvelopeRunner` owns the application processing loop.

After application processing succeeds, messaging acknowledges the incoming envelope through
`IncomingEnvelopeGateway`.

## Typing

Typing uses `GatewayClientMessage.TypingState`, `GatewayServerMessage.TypingState`, and
`GatewayTypingEvent`. It is transient and is not persisted in the protocol outbox.

## Gateway JSON

`createGatewayJson()` supplies the dedicated Kotlin Serialization configuration used for gateway
WebSocket frames. Do not use the SecureChat packet `Json` instance for these frames; the two sealed
hierarchies have separate compatibility boundaries.

## Extension rules

- Add gateway wire frames to `GatewayClientMessage`/`GatewayServerMessage` and update the matching
  server wire models.
- Keep explicit wire `@SerialName` values aligned between client and server.
- Use `/v1/gateway` as the canonical WebSocket endpoint.
- Keep contact/group routing resolution in `:feature:messaging`, not in transport.
- Keep packet meaning and encryption policy outside transport.
- Never interpret gateway acceptance as recipient delivery.
