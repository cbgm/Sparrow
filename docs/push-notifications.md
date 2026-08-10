# Push notifications

SecureChat uses the existing WebSocket while the Android UI is visible. When the UI is no longer visible, the WebSocket is disconnected and Firebase Cloud Messaging wakes the app for a short pending-message synchronization.

FCM carries only an opaque wake-up ID. The encrypted relay envelopes stay on the relay until the client processes and acknowledges them.

## Android Firebase setup

1. Create or select a Firebase project.
2. Add an Android application with package name `com.cbgm.securechat`.
3. Download `google-services.json` and place it at:

   ```text
   androidApp/google-services.json
   ```

4. Use an emulator or device with Google Play services.
5. Sync Gradle and reinstall the Android application.

The Google Services Gradle plugin is applied only when that file exists. This keeps the project buildable before local Firebase configuration is added, but push delivery remains disabled until it is present.

## Relay Firebase Admin setup

Create a Firebase service-account key for the same Firebase project. Keep the JSON file outside the repository.

PowerShell:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\secure\securechat-firebase-service-account.json"
.\gradlew.bat :relay:run
```

Git Bash:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/secure/securechat-firebase-service-account.json
./gradlew :relay:run
```

Without Application Default Credentials, the relay starts normally but logs that push delivery is disabled.

## Runtime flow

```text
App visible
  -> WebSocket connected
  -> envelope is processed and ACKed immediately

App backgrounded or process killed
  -> relay retains encrypted envelope
  -> relay waits briefly for a WebSocket ACK
  -> if still pending, relay sends high-priority FCM data wake-up
  -> FirebaseMessagingService schedules expedited Koin Worker
  -> Worker downloads all pending envelopes for the wake-up
  -> existing incoming-envelope processor stores them
  -> Worker ACKs each successfully processed envelope
  -> Android displays or updates one notification per conversation

Notification tap
  -> securechat://chat/{conversationId}
  -> shared notification navigation resolves direct or group chat
  -> exact conversation opens after startup completes
```

## Manual test

Use two Google Play-enabled emulators. The examples assume the receiver is `emulator-5556`.

1. Start the relay with Firebase Admin credentials.
2. Open SecureChat on both emulators and complete onboarding.
3. Confirm token registration:

   ```powershell
   curl http://localhost:8080/health
   ```

   `pushDevices` should be at least `2` after both clients have started and obtained FCM tokens.

4. Put the receiver in the background:

   ```powershell
   adb -s emulator-5556 shell input keyevent KEYCODE_HOME
   ```

5. Send a message from the other emulator.
6. After the relay fallback delay, the receiver should display a message notification.
7. Tap the notification. The exact direct or group conversation should open.

### Test process recreation

Kill only the process, not the Android package stopped state:

```powershell
adb -s emulator-5556 shell am kill com.cbgm.securechat
```

Then send another message. FCM should recreate the process, Koin should construct the Worker, and the notification should appear.

Do not use this command for the push-delivery test:

```powershell
adb -s emulator-5556 shell am force-stop com.cbgm.securechat
```

Android intentionally blocks background delivery after an explicit force-stop until the user opens the app again.

## Useful logs

Receiver:

```powershell
adb -s emulator-5556 logcat | findstr /I "FirebaseMessaging WorkManager PendingMessageSync SecureChat"
```

Relay:

```text
Firebase Admin is not configured
FCM wake-up failed
Push fallback failed
```

## Persistence

The federated `:server:push` service stores pending encrypted envelopes, wake-up mappings, and FCM
device tokens in its private PostgreSQL database. Docker Compose retains this database in the
`push-database-data` volume, so normal service and Compose restarts preserve offline delivery.

With multiple control planes configured, Android registers the same FCM token with every reachable
control plane. Gateway nodes keep one control plane as the notification primary for a new offline
envelope and replicate the opaque envelope to the remaining reachable control planes through the
replica endpoint, which deliberately does not send another FCM notification. Pending reads merge and
deduplicate replicas and acknowledgements are fanned out, so a control-plane failover does not lose
the offline queue or require the closed app to re-register first.

The migration-only `:relay` service still uses in-memory stores. Its messages survive client
disconnects but not a relay-process restart. Use the federated server topology for restart-durable
push delivery.
