# Push notifications

SecureChat uses the live gateway WebSocket while the app is active. Background/offline delivery uses
FCM only as a wake-up mechanism; message ciphertext stays in SecureChat server storage.

FCM does **not** carry message plaintext or the normal encrypted message body. It carries an opaque
wake-up identifier that lets the client retrieve pending work.

## Android Firebase setup

1. Create/select a Firebase project.
2. Add the Android application `com.cbgm.securechat`.
3. Put `google-services.json` at `androidApp/google-services.json`.
4. Use an emulator/device with Google Play services.
5. Rebuild/reinstall the app.

## Server Firebase Admin setup

Firebase Admin belongs to `:server:push`, not to the gateway. For the Docker topology, point
`FIREBASE_ADMIN_CREDENTIALS` in `server/.env` at the service-account JSON used by the push service,
then start the current server stack:

```powershell
docker compose -f server/docker-compose.yml up -d --build
```

Verify:

```powershell
curl.exe http://localhost:8095/health
```

The push health response should report `fcmEnabled=true` when Firebase Admin is configured.

## Runtime flow

### Foreground/live recipient

```text
sender
  -> TransportEnvelope
  -> gateway
  -> recipient WebSocket
  -> local processing
  -> envelope acknowledgement
```

### Offline/background recipient with mailbox route

```text
sender gateway
  -> federation
  -> recipient-selected mailbox stores encrypted federated envelope
  -> mailbox requests push wake-up
  -> FCM sends opaque wake-up ID
  -> Android worker retrieves mailbox envelope
  -> local processing
  -> mailbox acknowledgement
```

### Pending inbox

For peers that have not yet exchanged a mailbox route, the push service exposes the
pending-envelope inbox:

```text
GET  /push/wake/{wakeUpId}/inbox
POST /push/wake/{wakeUpId}/inbox/{envelopeId}/ack
```

The client adapter is `HttpPendingEnvelopeGateway` under `feature/transport/push/inbox`.

## Push-token registration

`HttpPushTokenRegistrationGateway` registers the local routing ID and FCM token with reachable
control-plane/push endpoints through:

```text
POST /push/devices
```

The push registration request serializes the client routing identifier as `routingId`.

## Manual test

1. Start the current Docker server topology with Firebase Admin configured.
2. Verify `http://localhost:8095/health` and at least one gateway health endpoint.
3. Open SecureChat on two Google Play-enabled emulators and complete onboarding.
4. Confirm push-device registration in push health/logs.
5. Put the receiving emulator in the background:

   ```powershell
   adb -s emulator-5556 shell input keyevent KEYCODE_HOME
   ```

6. Send a message from the other emulator.
7. Verify an FCM wake-up and message notification.
8. Tap the notification and confirm that the correct conversation opens.

To test process recreation without Android's explicit stopped state:

```powershell
adb -s emulator-5556 shell am kill com.cbgm.securechat
```

Do not use `am force-stop` for this test; Android intentionally suppresses background delivery after
an explicit force-stop until the app is opened again.

## Useful logs

Client:

```powershell
adb -s emulator-5556 logcat | findstr /I "FirebaseMessaging WorkManager PendingMessageSync SecureChat"
```

Server:

```powershell
docker compose -f server/docker-compose.yml logs --since=5m push mailbox federation gateway
```

## Persistence

`:server:push` persists device tokens, wake-up mappings and pending envelopes in its PostgreSQL database.

Mailbox-based offline ciphertext belongs to `:server:mailbox`; cross-node routing belongs to
`:server:federation`; the client WebSocket edge belongs to `:server:gateway`.
