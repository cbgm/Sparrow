# Transport security

Transport security has two layers:

1. the public HTTP/WSS transport provided by Caddy/TLS where configured;
2. application-level identity/group encryption and signatures, so server routing is not the sole confidentiality boundary.

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
