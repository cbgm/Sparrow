# Threat model

This is a concise engineering threat model for the current code, not a formal audit.

## Intended protections

The design aims to protect against:

- a server reading encrypted Direct/Group message plaintext in the normal encrypted path;
- an unauthenticated party forging registered Community Node requests;
- reading or deleting another user's envelopes (a mailbox answers only to the capability its owner created);
- posing as another device or phone alias in an invitation letter (letters are signed with the device key; a
  device routing ID must derive from it, and a phone alias must belong to it at the Presence Directory);
- trivial replay of signed server requests;
- silent substitution of a contact identity without surfacing security state;
- posing as a phone book contact by claiming their number in an invitation (only keys file an invitation
  under a known contact; a claimed number is shown as unverified, the invitation counts as unknown, and
  a matching phone book contact is linked only when the user accepts);
- getting around a block by writing the blocked number differently (`0151...`, `+49151...`) or by claiming it
  from a new installation (both resolve to the blocked contact; a sender that claims no number or another one
  still arrives as unknown, which "block unknown invites" covers);
- offline-message storage that requires server plaintext;
- accidental persistence of client private keys in plaintext Android preferences;
- taking over a phone alias another device holds (an alias stays with the first device that claims
  it; only an attestation of the Directory's phone verifier for that number moves it). The exception are the operator's
  configured test numbers, which any device can take;
- a node directory with an expired registry certificate, or one signed under a different registry
  root than the configured or first-seen one;
- a Community Node learning its users' phone numbers: the app claims its alias at each of its Control Planes,
  sealed for the presence directory's key from that Control Plane's signed node directory, and invitation letters
  are sealed for the recipient;
- Control Planes learning the app's network address: the app's requests to them go through two relays (onion
  layers sealed for each relay's key from the signed directory). The first relay sees the address but not the
  destination, the second the destination but not the address; neither reads the content. Two relays run by the
  same operator could link both, and with a single relaying node there is only one hop;
- looking up who uses Sparrow by hashing phone numbers cheaply: each invitation route lookup
  (`POST /v1/invitation-routes`) costs a proof of work, and the answer names no phone number.

Servers keep as little about users as they can: mailboxes store nothing about their creator (a new mailbox costs
a small proof of work instead of a per-address quota, so users behind one session node do not share a limit;
used work stamps are kept for 10 minutes), mailboxes live at most 31 days
and envelopes at most 7, request logs show route shapes (`/v1/mailboxes/{id}/envelopes`) instead of IDs, and
container logs are capped at 3 × 10 MB per service.

A mailbox node sees when envelopes are stored and fetched, and from that can guess which of its random mailboxes
form a group or the two directions of one chat, and when their owner is online. The optional decoy envelopes
(Settings > Network > Onion routing, off by default) hide which mailboxes a send was for; online status stays
visible. See [Decoy envelopes](../features/transport.md#decoy-envelopes).

Anyone can run a Community Node; registering one gives no power over other nodes' clients. The first relay of
a circuit still sees the app's network address. The last relay sees the content of requests that are not sealed
for the Control Plane (node directory fetches, invitation route lookups), but not who sent them.

## Infrastructure can still observe metadata

Depending on deployment, infrastructure can observe some metadata such as:

- source network addresses/connections;
- node/Control Plane access timing;
- routing IDs and delivery timing within the protocol's needs;
- encrypted envelope sizes;
- the first contact invite, which is unencrypted because the recipient's key is not known yet: the sender's
  phone number, public keys and invite challenge (no profile picture; it follows in the encrypted ready packet);
- group invites, join requests and their answers sent to a contact whose key is not known yet. Answers to a
  contact invite, and group invite packets to a contact with a known key, are encrypted;
- encrypted attachment/blob sizes and blob-transfer timing.

Phone verification at sign-up uses Firebase phone authentication on Android, so Google/Firebase
processes the phone number and sends the SMS. Only the operator's test numbers are bound without it.

Exchange rates for group expenses are fetched by the Community Node, not by the app, so the rate
provider (Frankfurter by default) sees the node's address once a day, never a user's.

Sparrow is not documented as an anonymity network.

## Local search and message safety

Semantic search and message-safety embedding/classification are designed to run locally on the Android client. The current implementation does not send message plaintext to a cloud AI service for those features. The embedding model itself is downloaded and integrity-checked locally.

## Device compromise

An unlocked/compromised device can undermine end-to-end security. Android Keystore wrapping protects key-at-rest handling but cannot make a fully compromised runtime trustworthy.

## Server compromise

Application-layer message encryption limits what a compromised routing server should learn about message content, but a compromised server can still disrupt availability, delay/drop traffic, serve stale data where signatures/expiry checks allow, or observe metadata. Trust/signature validation must therefore fail closed where required.

## Build/release compromise

The Android signing keystore and GitHub release secrets are high-value assets. Loss of the signing key prevents normal update continuity; compromise can allow malicious signed releases. Keep the `.jks` backed up offline and restrict GitHub secret/release permissions.

## Out of scope / unfinished

- iOS production security/runtime parity;
- a completed third-party security audit;
- protection from a malicious/compromised operating system on the client;
- metadata anonymity against all server operators.

## Comparison context

For a careful explanation of where federation can reduce central-infrastructure risk—and where Sparrow is **not** stronger than mature systems such as Signal—see [What makes Sparrow different?](../why-sparrow.md).
