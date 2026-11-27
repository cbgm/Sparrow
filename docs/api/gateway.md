# Gateway API

The gateway is the Community Node's edge for apps. Apps hold no session there: they reach every node through their
circuits and put messages into mailboxes themselves (see [Transport](../features/transport.md)). The Community Node
Caddy edge exposes the gateway under the node's public base address.

## Operator endpoints

Use `/index` first. Relevant links include:

- `/health/gateway` (`ok`)
- `/v1/control-planes`: the Control Planes this node advertises to apps (`ADVERTISED_CONTROL_PLANE_URLS`)

## Relays and circuits

```text
POST /v1/relay      one sealed onion layer for an HTTP request (OnionRelay)
GET  /v1/circuit    WebSocket: one hop of a circuit (CircuitRelay)
```

A relay passes a request on only to another relay and to the Control Plane, mailbox and blob paths the app uses.
The node's address in the node directory (`clientEndpoint`, `wss://host/v1/gateway`) is only an address: apps
derive the relay, circuit and blob URLs from it.

## Attachment blob HTTP API

```text
POST   /v1/blobs                         open a blob, paid with a proof of work
PUT    /v1/blobs/{blobId}/chunks/{index} upload one chunk
GET    /v1/blobs/{blobId}/chunks         the chunks that are there
GET    /v1/blobs/{blobId}/chunks/{index} download one chunk
DELETE /v1/blobs/{blobId}
```

Every file transfer runs over a circuit of its own, so the blob node does not see the device's address. The server
stores opaque encrypted bytes; attachment payload decryption and meaning stay on the client. Blob size,
retention/storage limits and cleanup are enforced by the gateway runtime.

## Exchange rates

```text
GET /v1/exchange-rates
```

Returns the euro reference rates as a JSON list in the Frankfurter format
(`[{"date": "...", "base": "EUR", "quote": "USD", "rate": 1.08}, ...]`), cached by the node for 24 hours
(`Cache-Control: public, max-age=3600`). `502` means the node has no rates yet and its upstream
(`EXCHANGE_RATES_URL`) failed. See [Community Node](../server/community-node.md#exchange-rates).
