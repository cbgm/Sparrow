# Play Console declaration for the connection service

`ConnectionService` is a foreground service of type `specialUse` (see [push-notifications.md](push-notifications.md)).
For a Play release it is declared under **App content → Foreground service permissions**, type **Special use**.
The texts below are ready to paste.

## Description of the task

> Sparrow is an end-to-end encrypted messenger that deliberately uses no push service (no Firebase Cloud Messaging).
> To receive messages while the app is closed, it keeps its own encrypted connection to the message servers the user
> chose, routed through relay nodes, and waits there for new messages. The foreground service holds that one
> connection open and does nothing else: no location, no media, no other data. It runs only if the user explicitly
> chooses "Always connected" during setup or in the network settings. The other choices, "Only while the app is open"
> and "Check now and then", don't use the service. While it runs, a notification ("Sparrow is connected – Messages
> arrive even while the app is closed") shows the user that it is active, and the user can switch it off at any time
> in the settings.

## Impact if the task is postponed or interrupted

> If the service is stopped, the user receives no messages until they open the app again. Messages then pile up
> unread on the servers, and the sender's message shows as not delivered. A push service would remove the need for
> the connection, but it would tell a third party (Google) when and how often the user receives messages, which
> Sparrow's privacy design avoids. WorkManager can't replace the service: it runs at most every 15 minutes and less
> often in deep sleep, so messages would arrive late. Sparrow offers that mode separately as "Check now and then" for
> users who accept the delay.

## Demo video

About 60–90 seconds, recorded on a real phone, uploaded to YouTube as **Unlisted**; the link goes into the form.

1. Open Sparrow for the first time and go through setup to the page **Receiving messages**.
2. Choose **Always connected (recommended)**. Show the battery list Android opens, then select Sparrow and set it to
   **Not optimized**.
3. Finish setup. The notification **Sparrow is connected** appears; pull down the notification shade to show it.
4. Close Sparrow by swiping it out of the recent apps. The notification stays.
5. On a second phone, send a message to this phone. Its notification arrives within seconds while Sparrow is closed.
6. Open **Settings → Network**, switch to **Only while the app is open**, and show the connection notification
   disappearing (the service stops).

## Before submitting

- The store listing briefly mentions that "Always connected" shows a permanent notification; Google compares the
  declaration with the listing.
- The Data safety form matches "no data shared with third parties for messaging"; no push token is sent anywhere.
- When the notification texts (`notification_connected*` in `resources/src/androidMain/res/values/strings.xml`) or
  the connection choices change, update the quotes above.
