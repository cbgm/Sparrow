# Gateway API

The Community Node Caddy edge exposes the gateway under the node's public base address.

## Operator endpoints

Use `/index` first. Relevant links include:

- `/health/gateway`
- `/v1/gateway/info`
- `/v1/control-planes`

The health response includes current gateway connection information used by operator/client diagnostics.

## Client WebSocket

```text
/v1/gateway
```

The client implementation is `DefaultWebSocketTransportClient`. Server handling is installed by `installGatewayRoutes()` and implemented by `GatewayWebSocketHandler`, `GatewaySessionHandler` and `GatewaySessionWorkDispatcher`.

The connection carries protocol-level client/server frames for registration, envelopes, acknowledgements, route refresh and ephemeral typing behavior. Treat the concrete models in `:server:protocol` and `:feature:transport` as the source of truth; do not handcraft JSON from this documentation for production code.

## Internal delivery

The gateway also participates in node-internal delivery from federation. Those endpoints are authenticated internal APIs and are not public client APIs.
