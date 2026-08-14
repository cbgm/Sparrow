# Server overview

Sparrow's production-shaped server is split into two independently deployable packages.

```mermaid
flowchart TB
    APP[Android clients]
    DIR[Control Plane directory JSON]

    subgraph CP[Control Plane]
        CADDY1[Caddy]
        REG[node-registry]
        PRES[presence-directory]
        REDIS[(Redis)]
        PUSH[push]
        PGREG[(PostgreSQL registry)]
        PGPUSH[(PostgreSQL push)]
    end

    subgraph NODE[Community Node]
        CADDY2[Caddy]
        GW[gateway]
        FED[federation]
        MB[mailbox]
        PGFED[(PostgreSQL federation)]
        PGMB[(PostgreSQL mailbox)]
    end

    APP --> DIR
    APP --> CADDY1
    APP --> CADDY2
    CADDY1 --> REG
    CADDY1 --> PRES
    CADDY1 --> PUSH
    REG --> PGREG
    PRES --> REDIS
    PUSH --> PGPUSH
    CADDY2 --> GW
    CADDY2 --> FED
    CADDY2 --> MB
    FED --> PGFED
    MB --> PGMB
    FED --> CADDY1
```

## Control Plane services

### `node-registry`

Stores signed Community Node descriptors and heartbeats. Public `/v1/nodes` returns currently healthy compatible
nodes. The registry uses PostgreSQL and signing authority material.

Key classes:

- `com.cbgm.sparrow.server.registry.ApplicationKt`
- `NodeRegistryStore`
- `PostgresNodeRegistryStore`
- `RegistrySigningRuntime`
- `RegistryDirectorySigner`

### `presence-directory`

Stores short-lived routing information mapping a client routing ID to its current node/connection. Redis is used
because presence is ephemeral and should be rebuilt by reconnecting clients.

Key classes:

- `PresenceRuntime`
- `installPresenceRoutes()`
- `RedisPresenceStore`
- `NodeRequestAuthorizer`

### `push`

Stores Android device registrations and wake-up/pending fallback information. It integrates with Firebase Admin
when credentials are configured.

Key classes:

- `PushRuntime`
- `PushCoordinator`
- `PostgresPushDeviceStore`
- `PostgresPendingEnvelopeStore`
- `PostgresWakeUpStore`
- `FirebasePushSender`

## Community Node services

### `gateway`

Owns client WebSockets, connection registry, route refresh handling, typing and envelope ingress/egress.

Key classes:

- `GatewayRuntime`
- `installGatewayRoutes()`
- `GatewayWebSocketHandler`
- `GatewaySessionHandler`
- `ConnectionRegistry`

### `federation`

Routes envelopes locally or to another node, resolves recipient presence, queues failed routes durably and retries.
It also registers/heartbeats the node against configured Control Planes.

Key classes:

- `FederationRouter`
- `FederationPeerRouter`
- `OutboundEnvelopeQueue`
- `PostgresOutboundEnvelopeStorage`
- `OutboundEnvelopeRetryAgent`
- `NodeRegistrationAgent`
- `CachingNodeRegistryClient`

### `mailbox`

Provides recipient-selected offline stores. Mailboxes are capability protected; encrypted envelopes can be stored,
retrieved, acknowledged and the entire mailbox can be revoked.

Key classes:

- `configureMailboxRoutes()`
- `MailboxStorage`
- `PostgresMailboxStore`
- `MailboxPushNotifier`

## Shared server modules

- `server:protocol` — HTTP/federation wire models.
- `server:security` — Ed25519 node identity/signatures, replay protection, rate limits, routing IDs.
- `server:persistence` — shared environment/idempotency helpers.
- `server:observability` — health/readiness, metrics, request IDs.

## Operator pages

Every launcher deployment exposes a `/index` page:

- Control Plane `/index`: registry, presence, push, connected nodes.
- Community Node `/index`: gateway health/info/connections, advertised planes, federation, mailbox.

See [Control Plane](control-plane.md) and [Community Node](community-node.md).

## Runtime class relationships

### Control Plane

```mermaid
classDiagram
    class RegistrySigningRuntime
    class RegistryDirectorySigner
    class NodeRegistryStore
    class PostgresNodeRegistryStore
    class PresenceRuntime
    class NodeRequestAuthorizer
    class PresenceStore
    class RedisPresenceStore
    class PushRuntime
    class PushCoordinator
    class PostgresPushDeviceStore
    class PostgresPendingEnvelopeStore
    class PostgresWakeUpStore
    class FirebasePushSender

    RegistrySigningRuntime --> RegistryDirectorySigner
    NodeRegistryStore <|.. PostgresNodeRegistryStore
    PresenceRuntime --> NodeRequestAuthorizer
    PresenceRuntime --> PresenceStore
    PresenceStore <|.. RedisPresenceStore
    PushRuntime --> PushCoordinator
    PushCoordinator --> PostgresPushDeviceStore
    PushCoordinator --> PostgresPendingEnvelopeStore
    PushCoordinator --> PostgresWakeUpStore
    PushCoordinator --> FirebasePushSender
```

### Community Node

```mermaid
classDiagram
    class GatewayRuntime
    class GatewayWebSocketHandler
    class GatewaySessionHandler
    class ConnectionRegistry
    class FederationRouter
    class FederationPeerRouter
    class OutboundEnvelopeQueue
    class PostgresOutboundEnvelopeStorage
    class OutboundEnvelopeRetryAgent
    class NodeRegistrationAgent
    class MailboxStorage
    class PostgresMailboxStore
    class MailboxPushNotifier

    GatewayRuntime --> GatewayWebSocketHandler
    GatewayWebSocketHandler --> GatewaySessionHandler
    GatewaySessionHandler --> ConnectionRegistry

    FederationRouter --> FederationPeerRouter
    FederationRouter --> OutboundEnvelopeQueue
    OutboundEnvelopeQueue --> PostgresOutboundEnvelopeStorage
    OutboundEnvelopeRetryAgent --> OutboundEnvelopeQueue
    NodeRegistrationAgent --> FederationPeerRouter

    MailboxStorage <|.. PostgresMailboxStore
    PostgresMailboxStore --> MailboxPushNotifier : store triggers notifier
```

The diagrams show responsibility/usage relationships, not every constructor parameter.
