# Docker and server operations

## Basic Docker commands

List containers:

```bash
docker compose ps
```

Follow logs:

```bash
docker compose logs -f
```

Recent logs:

```bash
docker compose logs --since=5m
```

Stop without deleting volumes:

```bash
docker compose down
```

Be careful with:

```bash
docker compose down --volumes
```

That deletes persistent data volumes.

## Health vs readiness

Server modules expose liveness/readiness through shared observability wiring. Caddy's friendly edge maps the most
important readiness checks to `/health/...` paths visible on `/index`.

A container being `Up` does not necessarily mean the application is ready; launcher/smoke scripts wait for
readiness.

## Smoke tests

Repository scripts:

```powershell
.\server\scripts\Test-SparrowNetwork.ps1 -Start -BuildImages
```

Standalone Control Plane + two independent Community Nodes:

```powershell
.\server\scripts\Test-StandaloneCommunityNodes.ps1 -BuildImages
```

The standalone smoke test verifies independent networks/volumes, stable node identities, signed registration
and that a node keeps its identity across a restart.

## Persistence

Durable:

- node registry PostgreSQL;
- mailbox PostgreSQL;
- node identity volumes;
- Community Node gateway attachment-blob volume (`gateway-blob-data`);
- registry signing/authority material.

Ephemeral/rebuildable:

- Redis phone alias bindings and invitation routes (apps claim them again every few hours).

## Backups

Use repository backup/restore scripts rather than copying live database files:

```text
server/scripts/Backup-Sparrow.ps1
server/scripts/Restore-SparrowBackup.ps1
```

For an installation created by the server installer, the Windows offline snapshot script is
`app/Backup-SparrowDeployment.ps1` in the installation folder (see `server/installer/README.md`).

## Troubleshooting sequence

1. Open deployment `/index`.
2. Check the failing health link.
3. `docker compose ps`.
4. Read logs for only the failing service first.
5. Check PostgreSQL/Redis health.
6. Check Caddy logs if the service is healthy internally but the public route returns 502.
7. For Community Node registration, verify the directory URL and `/v1/control-planes`.
8. For attachment failures, inspect gateway blob/ticket logs, blob volume capacity and configured size/retention values.
9. For client disconnects, inspect gateway health (`/v1/circuit`) and Android transport logs.
10. For a sign-up whose alias claim is rejected with `PHONE_VERIFICATION_FAILED`, check the
    `presence-directory` logs and that `CONTROL_PLANE_DIRECTORY_PUBLIC_KEY` is the key of the Directory whose
    phone verifier signed the attestation.
11. For a public installation with automatic sslip.io hostnames that became unreachable after the internet
    provider changed the public IP, run **Start / apply settings** in the installer: it moves the generated
    names to the new IP (previous settings in `backups/public-ip-*`). Custom DNS names must be updated at your
    DNS provider.

## Caddy 502 errors

A Caddy 502 usually means Caddy is running but its upstream container is unavailable/not ready. Check the upstream
service rather than changing public routes first.
