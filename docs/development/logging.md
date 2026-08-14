# Logging and diagnostics

## Client

Use the project's logging abstraction instead of scattered `println`. For network failures, the most useful path is usually:

1. Developer Settings network diagnostics;
2. selected/current node and cooldown state;
3. Control Plane status;
4. Android logcat around `DefaultWebSocketTransportClient` / transport connection manager;
5. corresponding node `/index` and Docker logs.

A node shown as `COOLDOWN` in Developer Settings must show `0` active connections; stale heartbeat counts are not presented as live connections.

## Server

Server modules use the shared `server:observability` facilities for request IDs, health/readiness and metrics/logging integration.

Useful commands:

```bash
docker compose ps
docker compose logs -f
docker compose logs --since=5m gateway federation mailbox
```

Start troubleshooting from `/index` on the Control Plane or Community Node so you do not need to remember endpoint paths.

## Secrets

Never log private keys, Firebase Admin credentials, release signing passwords, full bearer/internal tokens, mailbox capabilities or production database passwords.
