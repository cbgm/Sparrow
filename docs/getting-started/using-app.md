# Using the Android app

This is the current user flow for the Android client.

## 1. Complete onboarding

On first launch, follow the welcome/privacy/phone/permission pages and create the local Sparrow identity. The private identity material stays on the device; Android protects the stored private-key bytes through `AndroidPrivateKeyStorage`.

## 2. Add a contact

You can bring another Sparrow identity into the app through the sharing/import/QR flows. Device contacts can also be imported/linked where permission is granted.

Importing identity information is not the same as cryptographically verifying that person.

## 3. Handle contact invitations

Pending invitations appear in the invitation UI. You can:

- accept;
- decline;
- decline and block.

Blocking prevents normal contact interaction until unblocked.

## 4. Verify identity

Open contact details and compare the safety number, or use the QR verification path. Verification applies to the current public identity keys; if the identity changes, re-check it rather than assuming the old verification still applies.

## 5. Direct chat

Open the contact conversation and send messages. The UI exposes outgoing delivery state and supports retry when a send fails. Typing/read state is carried separately from the durable message body.

## 6. Create/use a Group

Create a group by selecting contacts. Group invitation/activation must complete before a member becomes an active recipient. Admins can manage membership/promotions according to the current group state. Group delivery/read progress is aggregated from recipient-specific delivery state.

## 7. Network settings

Settings contains Control Plane configuration. The Add field accepts:

- a single Control Plane URL; or
- a URL returning the JSON `controlPlanes` directory document.

The Developer Settings/network diagnostics show plane/node reachability, current node, connection counts and cooldown state.

## 8. Background delivery

When the app is not foregrounded normally, mailbox + Android FCM wake-up support can retrieve pending encrypted envelopes. Android Force stop disables normal background execution until the app is opened again.
