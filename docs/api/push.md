# Push API

The push service (`:server:push`) runs on the Control Plane behind Caddy. Push only wakes the app; the
payload is a short-lived wake-up ID, never message content.

## Register a device: `POST /push/devices`

Public, rate limited per client (`PUSH_DEVICE_REGISTRATION_RATE_LIMIT_*`). Binds one push token to the
caller's device routing ID.

```json
{
  "routingId": "scrouting1_…",
  "token": "<FCM token>",
  "platform": "ANDROID",
  "issuedAtEpochMilliseconds": 1767225600000,
  "clientSigningPublicKey": [ …32 bytes… ],
  "clientSignature": [ …64 bytes… ]
}
```

The signature is made with the identity signing key over a length-prefixed payload (4-byte big-endian
length before every field):

| Field | Value |
| --- | --- |
| domain | `sparrow-push-device-v1` |
| routing ID | UTF-8 |
| token | UTF-8 |
| platform | UTF-8 (`ANDROID`, `IOS`) |
| issued at | 8-byte big-endian epoch milliseconds |

Server: `ProtocolSignatures.pushDeviceContent`; app: `pushDeviceRegistrationPayload`. Both sides pin the
same byte vector in their tests.

The server checks, in this order (`PushDeviceRegistrationValidator`):

| Check | Status | `code` |
| --- | --- | --- |
| routing ID, token and platform present | 400 | `INVALID_PUSH_DEVICE` |
| routing ID is not a phone alias (`scphone1_…`) | 403 | `PUSH_ROUTING_ID_NOT_ALLOWED` |
| routing ID is derived from `clientSigningPublicKey` | 403 | `PUSH_ROUTING_ID_MISMATCH` |
| `issuedAtEpochMilliseconds` within ±5 minutes of server time | 403 | `PUSH_PROOF_STALE` |
| signature valid | 403 | `PUSH_PROOF_INVALID` |

Rejections carry `{"code": "…", "message": "…"}`. A request without proof fields (an old app) fails with
`PUSH_ROUTING_ID_MISMATCH`. Success is `204 No Content`.

Rules for stored bindings:

- One token belongs to exactly one routing ID: registering a token for another routing ID replaces the old
  binding.
- A binding expires `PUSH_DEVICE_REGISTRATION_LIFETIME_MILLISECONDS` (default 30 days) after its last
  registration. Expired rows are deleted before every device read or write and are never used for a
  wake-up. The app registers again on every start, every return to the foreground, every FCM token
  refresh and every 7 days in the background.
- Phone aliases cannot receive push. The app only registers its device routing ID.
- A binding also ends when FCM reports the token unregistered.

There is no public unregister or update endpoint; registration is the only public request that changes a
binding.

## Wake-up inbox

| Endpoint | Purpose |
| --- | --- |
| `GET /push/wake/{wakeUpId}/inbox` | pending envelopes of the woken recipient |
| `POST /push/wake/{wakeUpId}/inbox/{envelopeId}/ack` | delete one acknowledged envelope |

A wake-up ID is 32 bytes from `SecureRandom` (base64url), created per wake-up for one recipient, sent only
in the FCM data message to that recipient's registered devices and valid for
`PUSH_WAKE_UP_LIFETIME_MILLISECONDS` (default 15 minutes). An unknown or expired ID returns `404`.

## Node and internal endpoints

`/v1/node-push/*` need a signed Community Node request (and, for recipient data, a node that owns the
routing ID in the presence directory). `/internal/v1/*` need the internal API token. Neither changes
device bindings.
