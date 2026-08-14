# Control Plane

A Control Plane is the discovery/presence/push side of Sparrow. It does not carry normal client WebSocket
message traffic.

## Package contents

```text
Caddy
node-registry + PostgreSQL
presence-directory + Redis
push + PostgreSQL
secrets/configuration
```

## Windows launcher

In the release/generated bundle, double-click:

```text
Start-SparrowControlPlane.cmd
```

The PowerShell launcher:

- prevents concurrent launcher runs against the same deployment folder;
- starts Docker Desktop if required;
- shows LAN/Public configuration;
- persists `sparrow.conf`;
- prepares generated secrets;
- starts stateful stores before services;
- handles the known pre-release push schema migration/reset case;
- waits for registry, presence and push routes to become ready;
- reports detailed Docker state/logs on failure.

## LAN mode

The edge is exposed on port `8390` by default. Open:

```text
http://<host>:8390/index
```

## Public mode

Public mode uses the configured hostname/domain and Caddy. Automatic mode can derive an `sslip.io` hostname from
public IPv4; own-address mode uses the operator's hostname.

## `/index`

`/` redirects to `/index`. The page links to:

```text
/health/registry
/health/presence
/health/push
/v1/nodes
```

Use this page for normal operations instead of memorizing routes.

## Secrets

The bundle contains a visible `secrets/` directory. Firebase Admin credentials are required only for real FCM.
Registry signing/authority material is security-sensitive and should be backed up appropriately.

## macOS

The friendly Control Plane launcher bundle is currently Windows-only. On macOS, use Docker Desktop and the
repository's Control Plane Compose configuration from source. The service topology and `/index` page are the same.

## Control Plane directory

A Control Plane does not hardcode addresses of other planes. Apps and Community Nodes get the set of planes from
an external JSON directory. This keeps discovery changes outside application/server image source.
