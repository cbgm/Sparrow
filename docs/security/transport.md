# Transport security

Transport security has two layers:

1. the public HTTPS transport provided by Caddy/TLS where configured;
2. application-level identity/group encryption and signatures, so server routing is not the sole confidentiality boundary.

## Circuits and mailboxes

The app holds no session at a node and registers no route. It builds a circuit through relays of the signed node directory (`CircuitRunner`, `/v1/circuit`), sends requests to Control Planes through two relays (`/v1/relay`, onion layers sealed for each relay's key), and puts every packet into the recipient's mailbox. A mailbox is reached with a capability the recipient chose (`MailboxDeliveryRoute`), so no routing ID has to be signed in anywhere to send or receive. Envelopes carry hidden routing IDs.

A packet for a device whose mailbox route the sender does not have yet goes into the device's invitation mailbox: a letter signed with the device key and sealed for the recipient, deposited with a proof of work. The recipient accepts it only if the signature checks out, it is addressed to its own routing ID or alias, and the sender's routing ID derives from the signing key or (for a phone alias) belongs to that key according to the Presence Directory. See [Invitation mailboxes](../features/transport.md#invitation-mailboxes).

## Node registration

Community Nodes own long-lived node identities. They register signed descriptors and heartbeats with the node registry (`ProtocolSignatures.signDescriptor`/`verifyDescriptor`/`verifyHeartbeat`), and the registry signs the node directory the apps verify. Nodes do not call each other.

## Replay/rate controls

Server security includes bounded replay protection and request rate limiting. These controls reduce abuse and replay risk but are not a substitute for correct key management or perimeter security.

## Caddy

Caddy is the edge/reverse proxy. In public mode it is responsible for the externally reachable HTTP(S) endpoint and routes paths to the internal JVM services. Internal Docker service names/ports are not meant to be public application configuration.
