# Push API (pending envelopes)

Despite its name, the push service (`:server:push`) sends no push notifications any more: there is no
FCM and no device registration. It only keeps **pending envelopes** of the old delivery path through the
connected node, for a recipient that is not connected when a gateway or federation delivers to it. The
gateway hands them to the recipient when it signs in again, and the recipient acknowledges each one.
Mailbox delivery through circuits does not use this service; it goes away together with the old path.

## Node endpoints

Every `/v1/node-push/*` request is a signed Community Node request; a node may only store or read
envelopes for routing IDs whose presence route points at it.

| Endpoint | Purpose |
| --- | --- |
| `POST /v1/node-push/envelopes` | store one pending `TransportEnvelope` (`202`, or `507` when the store is full) |
| `POST /v1/node-push/replicas/envelopes` | store a copy held by another Control Plane |
| `GET /v1/node-push/recipients/{recipientId}/envelopes` | pending envelopes of a recipient |
| `POST /v1/node-push/recipients/{recipientId}/envelopes/{envelopeId}/ack` | delete one acknowledged envelope |

## Internal endpoints

`/internal/v1/envelopes`, `/internal/v1/recipients/{recipientId}/envelopes` and
`…/envelopes/{envelopeId}/ack` do the same for a gateway on the same host and need the internal API token
(`PUSH_INTERNAL_API_TOKEN`).

`GET /health` answers `ok persistence=<postgresql|memory> pendingEnvelopes=<n>`.
