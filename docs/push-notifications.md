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
| `ALWAYS_CONNECTED` | `ConnectionService` (foreground service, `specialUse`) | Messages arrive at once; a permanent notification shows that Sparrow is connected. Needs Sparrow set to "Not optimized" in the battery list. |
| `WHILE_OPEN` | none | Messages arrive while Sparrow is open; closed, they wait in the mailboxes until the next start. |
| `CHECK_PERIODICALLY` | `ConnectionCheckWorker` (every 15 minutes, connects for 45 s) | Offered in the settings only. Messages arrive with a delay. |

`ConnectionModeApplier` starts or stops the service and the worker when the mode changes, and
`ConnectionBootReceiver` applies it again after a reboot. iOS cannot keep a connection while the app is
closed (`keepsConnectionWhileClosed()` is false there); the onboarding page shows a note instead of the
choice.

Choosing `ALWAYS_CONNECTED` (preselected in onboarding, where swiping on past the page counts as choosing it) opens Android's battery optimization list (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`), where
the user sets Sparrow to "Not optimized"; the onboarding page says so. Sparrow does not ask for the exemption
directly (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`): Google Play allows that only for apps that technically cannot use
its push service, and Sparrow does without it by choice. For a Play release, the `specialUse` foreground service
needs a declaration under App content in the Play Console (description, impact of an interruption, demo video); the
texts and the video script are in [play-console-declaration.md](play-console-declaration.md).

Some makers stop apps in the background on their own (Xiaomi, Huawei, Honor, Oppo, Realme, OnePlus, Vivo, Samsung,
Asus). On those phones, `BackgroundRestrictionHelp` (core:ui) shows one line on what to set and a button that opens the
maker's own setting, or Sparrow's app settings when that screen cannot be opened; `PhoneMaker` holds the makers and
their screens. On an emulator, a debug build can pretend to be one of these makers:

```
adb devices
adb -s emulator-5554 shell "run-as com.cbgm.sparrow sh -c 'echo xiaomi > files/debug_phone_maker'"
adb -s emulator-5554 shell "run-as com.cbgm.sparrow rm files/debug_phone_maker"
```

`adb devices` lists the running emulators; `-s` picks one (needed when several run). The double quotes keep the `>` on
the device in both cmd and PowerShell. The second line sets the maker (`xiaomi`, `huawei`, `honor`, `oppo`, `vivo`, `samsung`, `asus`), the second goes back.
The emulator has none of the makers' screens, so the button opens Sparrow's app settings there.

## Servers on the local network

Apps targeting Android 17 need the runtime permission `ACCESS_LOCAL_NETWORK` to reach local network addresses;
without it, connections to them time out. `LocalNetworkAccess` in the app shell asks for it once per app start while
the Control Plane directory, a Control Plane or a node from the node directory has a local address
(`ObserveLocalNetworkNeededUseCase`, `LocalNetworkAddresses`: private and link-local IPs, `.local` names and
sslip.io/nip.io names carrying a private IPv4). A refusal shows a short note that local servers cannot be reached.
Public servers are not affected.

While the process runs without a screen (service or worker), `HeadlessStartPort` starts only what receiving
needs, and `ConnectionKeepAlivePort` holds the connection open.

## Offline delivery

A sender puts the encrypted envelope into the recipient's mailbox through its own circuit (see
[Transport](features/transport.md)). The recipient's app waits at its mailboxes through its circuit
(`POST /v1/mailboxes/wait`) and fetches what arrived; mailbox envelopes enter the same incoming pipeline as
every other envelope; nothing else delivers messages. Nothing wakes the device from outside: a closed app in `WHILE_OPEN` mode fetches
the envelopes on its next start.

## Phone verification

Firebase is used only for the phone sign-in at sign-up. The app has no `google-services.json`; its
Firebase project comes from `local.properties` (see [Local development](development/local-development.md)).
The Control Plane Directory verifies the number (see [Control Plane](server/control-plane.md)).

## Force stop caveat

Android's **Force stop** stops the service and the periodic check until the user opens the app again.
Test receiving in the background by leaving the app normally, not with Force stop.
