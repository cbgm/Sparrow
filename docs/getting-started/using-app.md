# Using the Android app

This is the current user flow for the Android client.

## 1. Complete onboarding

On first launch, follow the welcome/privacy/phone/permission pages and create the local Sparrow identity. The private identity material stays on the device; Android protects the stored private-key bytes through `AndroidPrivateKeyStorage`.

## 2. Add a contact

You can bring another Sparrow identity into the app through the sharing/import/QR flows. Device contacts can also be imported/linked where permission is granted. Importing identity information is not the same as cryptographically verifying that person.

## 3. Handle contact invitations

Pending invitations can be accepted, declined, or declined and blocked. Blocking prevents normal contact interaction until unblocked.

## 4. Verify identity

Open contact details and compare the safety number, or use the QR verification path. Verification applies to the current public identity keys; if the identity changes, re-check it.

## 5. Direct chat

Open the contact conversation and send text or attachments. The UI exposes outgoing delivery state and supports retry when a send fails. Typing/read state is carried separately from the durable message body.

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

Settings contains Control Plane configuration. The Add field accepts either a single Control Plane URL or a URL returning the JSON `controlPlanes` directory document. Developer/network diagnostics show plane/node reachability, current node, connection counts and cooldown state. Developer Settings also contains a persisted timestamped error log that can be cleared.

## 11. Background delivery

When the app is not foregrounded normally, mailbox + Android FCM wake-up support can retrieve pending encrypted envelopes. Android Force stop disables normal background execution until the app is opened again.
