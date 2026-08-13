# Local Development and Manual Testing

This guide describes a local Android/emulator setup against the current federated server stack.
The deleted standalone `:server:gateway` application is no longer used.

## Prerequisites

- Windows or another Docker-supported development host
- Docker Desktop / Docker Engine
- Android Studio and Android SDK
- one or more Android emulators
- the Java/Gradle requirements in [Installation](../getting-started/installation.md)

## 1. Start the server network

From the repository root:

```powershell
docker compose -f server/docker-compose.yml up -d --build
```

The default local services include node registry, presence, mailbox, federation, gateway and push.
Useful health endpoints are:

```text
http://localhost:8090/health   node registry
http://localhost:8091/health   presence
http://localhost:8092/health   mailbox
http://localhost:8093/health   federation
http://localhost:8094/health   gateway
http://localhost:8095/health   push
```

For a second independent node, also apply `server/docker-compose.multinode.yml`; see
[`server/README.md`](../../server/README.md).

## 2. Verify gateway discovery

The gateway still exposes the compatibility WebSocket endpoint `/relay`, but clients obtain gateway
endpoints from verified node/control-plane discovery instead of relying on a hard-coded gateway service.

Check at minimum:

```powershell
curl.exe http://localhost:8090/health
curl.exe http://localhost:8094/health
```

If push/background delivery is part of the test:

```powershell
curl.exe http://localhost:8095/health
```

## 3. Run the Android clients

Build/install normally through Android Studio or:

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

For multiple emulators, give each installation its own application data/identity. Complete onboarding
independently and verify that each client connects to a discovered gateway.

## 4. Typical manual test flow

1. Start the server network.
2. Verify registry and gateway health.
3. Start two or three emulators.
4. Launch SecureChat on each device and complete onboarding.
5. Exchange/accept identities or invitations as required by the scenario.
6. Send direct and group messages in both directions.
7. Verify sent, delivered and read states.
8. Verify typing while both clients are online.
9. Disconnect one client and send another message.
10. Reconnect it and verify queued/offline delivery.
11. For multi-node tests, stop one gateway and verify failover to another verified node.

## Troubleshooting

### Client cannot connect

Check:

- registry/control-plane discovery is reachable;
- at least one advertised gateway health endpoint is healthy;
- the advertised endpoint is reachable from the emulator/device;
- the gateway WebSocket process is running;
- firewall/network rules permit the connection.

The `/relay` path is an external compatibility endpoint and should not be renamed as part of the
client package cleanup.

### Message remains queued

Check:

- the sender has an active gateway connection;
- the recipient routing ID and transport key can be resolved;
- federation/mailbox/push health is good when the recipient is on another node or offline;
- gateway/federation logs do not show routing/authentication failures.

### Inspect logs

```powershell
adb -s emulator-5554 logcat
adb -s emulator-5556 logcat
```

Server logs:

```powershell
docker compose -f server/docker-compose.yml logs --since=5m gateway federation mailbox push
```

## Related documentation

- [Gateway API](../api/gateway.md)
- [WebSocket API](../api/websocket.md)
- [Transport](../features/transport.md)
- [Testing](testing.md)
