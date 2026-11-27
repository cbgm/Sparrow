# Using the Android app

This is the current user flow for the Android client.

## 1. Complete onboarding

On first launch, swipe through the pages (welcome, how relays hide who you talk to, permissions, how messages arrive while Sparrow is closed, light or dark), enter your name and phone number, confirm the SMS code and create the local Sparrow identity (or restore it from a backup). Afterwards an optional six-step tour shows the contact ring, the top bar, delivery dots, holding a message, the date rail and swiping on a sample chat. The private identity material stays on the device; Android protects the stored private-key bytes through `AndroidPrivateKeyStorage`. See [Onboarding](../features/onboarding.md).

## 2. Add a contact

You can bring another Sparrow identity into the app through the sharing/import/QR flows. Device contacts can also be imported/linked where permission is granted. Importing identity information is not the same as cryptographically verifying that person.

## 3. Handle contact invitations

Pending invitations can be accepted, declined, or declined and blocked. Blocking prevents normal contact interaction until unblocked.

## 4. Verify identity

Open contact details and compare the safety number, or use the QR verification path. Verification applies to the current public identity keys; if the identity changes, re-check it.

## 5. Direct chat

Start a chat with the pen button on the Chats screen, or open the contact conversation, and send text or attachments. The UI exposes outgoing delivery state and supports retry when a send fails. Typing/read state is carried separately from the durable message body.

The attachment bar supports:

- gallery image/video selection;
- camera capture;
- files;
- a shared contact;
- current location.

Media can be opened in the message viewer; video playback starts only after user action. Files can be opened after the local/downloaded path is available. Location opens through the platform location handler. Contact sharing reuses the Contacts screen; tapping the received contact can add it to device contacts after confirmation.

## 6. Create/use a Group

Create a group by selecting contacts. Group invitation/activation must complete before a member becomes an active recipient. Admins can manage membership/promotions according to the current group state. Group delivery/read progress is aggregated from recipient-specific delivery state. Group messages support the same attachment types as Direct messages.

## 7. Search messages

Message search always supports local exact matching. If semantic search is enabled and its local model/index are ready, Sparrow also adds semantic matches. Selecting a result navigates to the relevant Direct/Group message.

## 8. Optional message safety

Settings can enable local message-safety analysis. Messages with detected reasons can show a warning/details flow. This analysis is local and should be treated as a warning aid rather than a definitive classification.

## 9. Attachment storage

Settings includes attachment storage/management. Incoming image/video/file copies are grouped by conversation. Location/contact structured payloads are not presented as saved media/files.

## 10. Network settings and diagnostics

Settings → Network contains the Control Planes, the preferred node and onion routing (hops, circuits, decoy envelopes and whether to send directly while no route is available; see [Transport](../features/transport.md#onion-circuits)). The Control Plane screen's Add field accepts either a single Control Plane URL or a URL returning the JSON `controlPlanes` directory document. Developer/network diagnostics show plane/node reachability, current node and cooldown state. Developer Settings also contains a persisted timestamped error log that can be cleared.

## 11. Background delivery

How messages arrive while Sparrow is closed depends on the connection mode chosen under *Receiving messages*: always connected, while open (they wait in the mailboxes until the next start), or checking every 15 minutes. See [Receiving messages](../push-notifications.md). Android Force stop disables normal background execution until the app is opened again.

## 12. Privacy and data

Settings → Privacy & data shows the privacy policy and the data disclaimer: what stays on the phone, what the servers keep and for how long, and what they can still see. To delete everything on the phone, clear Sparrow's storage in Android's app settings or uninstall it; that cannot be undone.
