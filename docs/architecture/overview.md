# Architecture overview

Sparrow is a Kotlin Multiplatform secure-messaging client plus a federated Kotlin/Ktor server system. The current codebase separates **feature ownership** from **cross-feature orchestration** so repositories do not become hidden coordinators.

## High-level client architecture

```mermaid
flowchart TD
    UI[Compose presentation]
    UC[feature domain use cases]
    ORCH[feature:conversationorchestration]
    INV[feature:invite]
    ID[feature:identity]
    MEM[feature:membership]
    CHAT[feature:chats]
    ATT[feature:attachments / media / voice / linkpreview]
    MSG[feature:messaging]
    TR[feature:transport]
    PROTO[core:protocol]
    CRYPTO[core:crypto]
    DB[(data:database)]

    UI --> UC
    UC --> CHAT
    UC --> INV
    UC --> ID
    UC --> MEM
    CHAT --> ORCH
    ORCH --> CHAT
    ORCH --> INV
    ORCH --> ID
    ORCH --> MEM
    ORCH --> MSG
    MSG --> TR
    CHAT --> PROTO
    ORCH --> PROTO
    MSG --> PROTO
    CHAT --> DB
    INV --> DB
    ID --> DB
    MEM --> DB
    ATT --> DB
    PROTO --> CRYPTO
```

## Current ownership boundaries

| Module | Owns |
|---|---|
| `:feature:chats` | Direct/Group conversations, messages, reactions, edits/deletes, receipts, Group pins, chat UI |
| `:feature:invite` | Generic Direct/Group invitation lifecycle and invitation inbox UI |
| `:feature:identity` | Local identity, identity exchange/trust, backup/restore, pending remote identity changes, approved reconnections |
| `:feature:membership` | Group membership, roles, Group security epochs/keys, welcome/activation/admin/leave/delete membership lifecycle |
| `:feature:conversationorchestration` | Workflows that must coordinate chats + invite + identity + membership + transport |
| `:feature:messaging` | Generic durable outbox and incoming-envelope execution |
| `:feature:transport` | Control Plane discovery, Community Node selection, WebSocket/presence/mailbox/push transport |
| `:feature:attachments` | Attachment preparation/loading/cache/storage/transcripts |
| `:feature:voice` | Voice recording/playback/local transcription on top of attachments |
| `:feature:linkpreview` | Client link-preview cache/fetch/rendering |
| `:feature:autoreply` | Auto-reply configuration and per-contact claiming |

## Application startup

`SparrowApplication` initializes DI. `shared.presentation.AppViewModel` owns application startup and runtime coordination. It:

1. initializes crypto/settings/notification prerequisites;
2. restores and refreshes the signed Control Plane directory;
3. starts Control Plane health maintenance;
4. waits for local identity readiness;
5. starts orchestration observers (`InvitationResultObserver`, `MembershipResultObserver`, `MessagingTransportResultObserver`, `IdentityResultObserver`, `ContactBlockObserver`, `ApprovedIdentityReconnectionObserver`);
6. synchronizes device contacts when permission is available;
7. starts/stops foreground network runtime based on visibility.

Foreground runtime uses `IncomingEnvelopeRunner`, `TransportConnectionManager`, `OutboxRunner` and mailbox coordination.

See [Runtime orchestration and protocol flows](runtime-flows.md).

## Feature-internal layering

```mermaid
flowchart TD
    P[Presentation\nCompose / ViewModel / *Ui] --> U[Domain use cases]
    U --> R[Domain repository contracts]
    RI[Data repository implementation] --> R
    RI --> DS[Datasource / persistence / network adapter]
    DS --> EXT[(Room / DataStore / Ktor / platform API)]
```

Current rules:

- ViewModels call use cases rather than DAOs/datasources/repository implementations.
- A domain use case **may compose other use cases** when that is the intended orchestration boundary. This is used heavily by `:feature:conversationorchestration` and some feature-level workflows.
- Repository implementations do not call unrelated repositories or use cases.
- Datasources do not call repositories.
- Cross-feature coordination belongs in orchestration/use-case composition, not repository chaining.
- Presentation uses `...Ui`; data transport/intermediate representations use `...Dto`; persistence uses `...Entity`.
- Platform code belongs in the owning module's platform source set.

## Direct vs Group

Direct and Group conversation semantics stay distinct. They may share truly generic infrastructure (packet decoding, persisted outbox, attachment loading, overview projection), but authorization, recipient selection, membership and Group epoch security are separate.

## Persistence

The Room database is currently schema version **53** and contains durable state for invitations, identity exchange/recovery, group membership/security, messages/attachments, protocol outbox, mailbox routes, link previews, search/safety and auto reply.

See [Persistence model](persistence.md).

## Server topology

```mermaid
flowchart LR
    APP[Client]
    subgraph CP[Control Plane]
      C1[Caddy]
      REG[node-registry]
      PRES[presence-directory]
      PUSH[push]
    end
    subgraph CN[Community Node]
      C2[Caddy]
      GW[gateway]
      FED[federation]
      MB[mailbox]
    end
    LP[link-preview service]

    APP --> C1
    APP <--> C2
    C1 --> REG
    C1 --> PRES
    C1 --> PUSH
    C2 --> GW
    C2 --> FED
    C2 --> MB
    GW <--> FED
    FED --> MB
    APP --> LP
```

The deployable runtime and unified installer are described in [Server runtime, build and deployment](../server/runtime-build-deployment.md).
