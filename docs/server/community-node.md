# Community Node

A Community Node accepts client WebSockets, federates traffic to other nodes, stores recipient-selected offline
mailboxes, and hosts opaque encrypted attachment blobs through the gateway.

## Package contents

```text
Caddy
gateway + persistent attachment blob volume
federation + PostgreSQL
mailbox + PostgreSQL
persistent node identity
sparrow.conf
launchers
```

## Windows

Run:

```text
Start-SparrowNode.cmd
```

The launcher opens configuration with:

- LAN/Public mode;
- automatic public `sslip.io` or own public address;
- Control Plane directory URL.

## macOS/Linux

Run:

```bash
./start-sparrow-node.sh
```

or on macOS open:

```text
Start-SparrowNode.command
```

## Directory behavior

The directory body is explicitly parsed as JSON regardless of HTTP `Content-Type`.

A previously configured node keeps its last known Control Plane addresses. If every plane is currently offline,
the node starts and `NodeRegistrationAgent` keeps retrying. A fresh node with no cached plane addresses keeps
retrying the directory instead of exiting.

## LAN mode

Default edge port:

```text
8490
```

Client gateway URL is advertised as:

```text
ws://<host>:8490/v1/gateway
```

## Public mode

The node advertises HTTPS/WSS on the configured public host.

## Attachment blobs

Caddy forwards `/v1/blobs/*` to the gateway. Uploads use short-lived WebSocket-issued upload tickets; downloads/deletes use per-blob capabilities. The gateway stores only the encrypted blob bytes and metadata needed for size/expiry/capability enforcement.

The Community Node Compose package mounts a persistent `gateway-blob-data` volume at `/var/lib/sparrow/blobs`. Default per-blob/server storage limits come from `BLOB_MAXIMUM_BYTES` / `BLOB_MAXIMUM_STORAGE_BYTES` and can be overridden by deployment configuration. Cleanup removes expired blobs.

## `/index`

Open:

```text
http://<host>:8490/index
```

The page links to:

```text
/health/gateway
/v1/gateway/info
/v1/control-planes
/health/federation
/v1/federation/capabilities
/health/mailbox
```

Gateway health includes the current connection count, which is also reported in node heartbeats for client
Developer Settings diagnostics.
