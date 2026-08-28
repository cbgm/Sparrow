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

## Attachment blob HTTP API

The Community Node gateway also exposes encrypted attachment blob operations through Caddy:

```text
PUT    /v1/blobs/{blobId}
GET    /v1/blobs/{blobId}
DELETE /v1/blobs/{blobId}
```

Uploads require a short-lived bearer permit first requested over the authenticated gateway WebSocket (`RequestBlobUploadTicket`). The permit is bound to the blob ID, maximum size and expiry and is consumed by the upload. Downloads/deletes use the blob capability carried in the attachment's encrypted blob reference.

The server stores opaque encrypted bytes; attachment payload decryption/meaning stays on the client. Blob size, retention/storage limits and cleanup are enforced by the gateway runtime.

## Internal delivery

The gateway also participates in node-internal delivery from federation. Those endpoints are authenticated internal APIs and are not public client APIs.
