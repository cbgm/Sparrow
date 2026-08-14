# Threat model

This is a concise engineering threat model for the current code, not a formal audit.

## Intended protections

The design aims to protect against:

- a server reading encrypted Direct/Group message plaintext in the normal encrypted path;
- an unauthenticated party forging registered Community Node requests;
- trivial replay of signed server requests;
- silent substitution of a contact identity without surfacing security state;
- offline-message storage that requires server plaintext;
- accidental persistence of client private keys in plaintext Android preferences.

## Infrastructure can still observe metadata

Depending on deployment, infrastructure can observe some metadata such as:

- source network addresses/connections;
- node/Control Plane access timing;
- routing IDs and delivery timing within the protocol's needs;
- node connection counts/load;
- encrypted envelope sizes.

Sparrow is not documented as an anonymity network.

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
