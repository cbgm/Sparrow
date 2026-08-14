# FAQ

## Is there an official downloadable release?

Not yet. The workflow is ready to create individual assets plus a combined `securechat-<version>-full.zip`, but no official `v*` tag has been published at the time of this documentation update.

## Which client is usable?

Android. iOS source sets/Xcode host exist, but major runtime/platform functionality is still missing; iOS is not currently supported.

## How do I know whether a server is healthy?

Open `/index` on it. Control Plane `/index` links registry/presence/push/node-directory status; Community Node `/index` links gateway, control-plane list, federation and mailbox status.

## Why does the app use a Control Plane directory URL?

To avoid hardcoding deployment plane addresses. The directory returns a JSON `controlPlanes` array. Its HTTP content type may be `text/plain` or `application/json` because the body is parsed explicitly as JSON.

## Can Settings add both a single plane and a directory?

Yes. The Add field attempts to parse the entered URL as a Control Plane directory; if it is not a valid directory document it is treated as a single manual plane URL.

## What happens if a Community Node starts while Control Planes are offline?

An already-configured node can start with cached plane addresses and keeps retrying. A fresh node without cached addresses keeps retrying the configured directory instead of exiting.

## Why is a dead node shown as COOLDOWN?

So Developer Settings shows the recent failure instead of making the row disappear immediately. Dead/cooldown nodes are excluded from routing and show `0` connections.

## Does the server read chat plaintext?

Normal encrypted Direct/Group delivery is designed so gateway/federation/mailbox route or store opaque encrypted data. Client code owns packet decryption/meaning. Infrastructure still observes some metadata; see [Threat model](security/threat-model.md).

## Are Direct and Group chats implemented by the same repository?

No. This is intentional. They have different repositories, outgoing/incoming paths, delivery state machines and typing/membership logic.

## Are attachments implemented?

No complete attachment feature is documented as working currently. Do not advertise planned roadmap items as current functionality.

## How are releases built?

Create `release/x.y` from `master`. Release-branch pushes create change-aware candidate artifacts. A `v*` tag on the release line triggers the complete GitHub release. See [Release process](development/release-process.md).

## Where are generated architecture docs?

`docs/generated/`. Regenerate with `./gradlew architectureReport`; do not edit those pages manually.
