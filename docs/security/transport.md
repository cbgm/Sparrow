# Transport security

Transport security has two layers:

1. the public HTTP/WSS transport provided by Caddy/TLS where configured;
2. application-level identity/group encryption and signatures, so server routing is not the sole confidentiality boundary.

## Client session sign-in

A WebSocket session is bound to a routing ID only after challenge-response: the gateway sends a single-use 32-byte nonce and its node ID, and the client signs a domain-separated, length-prefixed payload over routing ID, connection ID, node ID and nonce with its identity signing key. The gateway checks that the routing ID is derived from the presented key and that the signature is valid for this connection's nonce and its own node ID (`GatewaySessionValidator`, `ProtocolSignatures.verifyGatewaySession`); otherwise it answers `INVALID_SESSION` and closes the connection. The client in turn refuses a challenge from a node other than the one it chose from the signed directory. Without a valid proof nobody can receive, acknowledge (delete) or send envelopes as another routing ID. See [Session sign-in](../api/websocket.md#session-sign-in-challenge-response).

## Push device registration

`POST /push/devices` binds a push token to a routing ID only with a proof: the client signs a
domain-separated (`sparrow-push-device-v1`), length-prefixed payload over routing ID, token, platform and
issue time with its identity signing key. The push service requires a non-alias routing ID derived from the
presented key, an issue time within ±5 minutes and a valid signature (`PushDeviceRegistrationValidator`,
`ProtocolSignatures.verifyPushDevice`). A token is bound to one routing ID at a time and the binding
expires 30 days after the last registration. Wake-up IDs are 256-bit random values valid for 15 minutes.
See [Push API](../api/push.md).

## Client route registration

Clients register a signed, expiring presence route through the Community Node gateway. Route refreshes include freshness/generation checks. The gateway validates them with `GatewayRouteValidator`; the Control Plane presence service parses the signed request with `nodeRequestAuthentication()` and authorizes it through `PresenceRuntime.nodeRequestAuthorizer` (`NodeRequestAuthorizer`).

## Node-to-node requests

Community Nodes own long-lived node identities. Federation/control requests are signed and verified using `server:security` helpers such as:

- `NodeRequestAuthentication`;
- `NodeRequestAuthorizer`;
- `ProtocolSignatures`;
- `ReplayProtection`.

The signed request context covers the request method/path plus freshness/nonce/body-related data defined by the protocol implementation, so an arbitrary HTTP caller cannot impersonate a registered node merely by knowing the endpoint URL.

## Replay/rate controls

Server security includes bounded replay protection and request rate limiting. These controls reduce abuse and replay risk but are not a substitute for correct key management or perimeter security.

## Caddy

Caddy is the edge/reverse proxy. In public mode it is responsible for the externally reachable HTTP(S)/WSS endpoint and routes paths to the internal JVM services. Internal Docker service names/ports are not meant to be public application configuration.
