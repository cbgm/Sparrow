# Receiving messages and offline delivery

Sparrow uses no push service (no FCM, no Firebase Cloud Messaging). The app keeps its own connection
through its circuit and waits at its mailboxes; nothing outside the user's device and the nodes it chose
learns when a message arrives.

## Connection modes

The user chooses how messages reach the device while Sparrow is closed (onboarding page *Receiving
messages*, later under the network settings). The choice is stored as `ConnectionMode` in the app's
settings (`transport.connection_mode`).

| Mode | Android | Effect |
| --- | --- | --- |
| `ALWAYS_CONNECTED` | `ConnectionService` (foreground service, `specialUse`) | Messages arrive at once; a permanent notification shows that Sparrow is connected. Needs the battery exemption. |
| `WHILE_OPEN` | none | Messages arrive while Sparrow is open; closed, they wait in the mailboxes until the next start. |
| `CHECK_PERIODICALLY` | `ConnectionCheckWorker` (every 15 minutes, connects for 45 s) | Offered in the settings only. Messages arrive with a delay. |

`ConnectionModeApplier` starts or stops the service and the worker when the mode changes, and
`ConnectionBootReceiver` applies it again after a reboot. iOS cannot keep a connection while the app is
closed (`keepsConnectionWhileClosed()` is false there); the onboarding page shows a note instead of the
choice.

While the process runs without a screen (service or worker), `HeadlessStart` starts only what receiving
needs, and `ConnectionKeepAlive` holds the connection open.

## Offline delivery

A sender puts the encrypted envelope into the recipient's mailbox through its own circuit (see
[Transport](features/transport.md)). The recipient's app waits at its mailboxes through its circuit
(`POST /v1/mailboxes/wait`) and fetches what arrived; mailbox envelopes enter the same incoming pipeline as
every other envelope. Nothing wakes the device from outside: a closed app in `WHILE_OPEN` mode fetches
the envelopes on its next start.

The Control Plane's `push` service only keeps pending envelopes of the old delivery path through the
connected node (see [Push API](api/push.md)). It goes away together with that path.

## Phone verification

Firebase is used only for the phone sign-in at sign-up. The app has no `google-services.json`; its
Firebase project comes from `local.properties` (see [Local development](development/local-development.md)).
The Control Plane Directory verifies the number (see [Control Plane](server/control-plane.md)).

## Force stop caveat

Android's **Force stop** stops the service and the periodic check until the user opens the app again.
Test receiving in the background by leaving the app normally, not with Force stop.
