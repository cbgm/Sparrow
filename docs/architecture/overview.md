# Architecture overview

Sparrow is a Kotlin Multiplatform secure-messaging client plus a federated Kotlin/Ktor server system. The current codebase separates shared content models, feature ownership and explicit cross-feature orchestration.

## High-level client architecture

```mermaid
flowchart TD
    UI[Compose presentation]
    BASE[core:base
MessagePart hierarchy]
    UC[feature domain use cases]
    API[feature:conversationorchestration:api]
    ORCH[feature:conversationorchestration
engine / delivery / message / groupstate / membership / lifecycle / incoming]
    CHAT[feature:chats]
    POLL[feature:polls]
    ATT[feature:attachments / media / voice]
    MSG[feature:messaging]
    TR[feature:transport]
    PROTO[protocol]
    DB[(data:database v55)]

    UI --> UC
    UI --> BASE
    UC --> CHAT
    UC --> POLL
    CHAT --> BASE
    POLL --> BASE
    CHAT --> API
    ORCH --> API
    CHAT --> ATT
    ATT --> DB
    CHAT --> DB
    CHAT --> PROTO
    ORCH --> DB
    ORCH --> PROTO
    PROTO --> MSG
    MSG --> TR
```

Chats submits every write as a `ConversationCommand` to `ConversationOrchestrator`; the orchestration modules execute it as a workflow state machine. See [Conversation orchestration](conversation-orchestration.md).

## Current ownership boundaries

| Module | Owns |
|---|---|
| `:core:base` | shared message-part data/domain/UI models and common domain primitives |
| `:protocol` | application packets/codecs and `OperationMessage` |
| `:feature:chats` | Direct/Group conversation presentation and read-only data access; submits writes as commands |
| `:feature:polls` | poll creation and poll-specific presentation |
| `:feature:attachments` | encrypted blob transfer/cache/storage and attachment loading |
| `:feature:media` | media/file selection, rendering and export |
| `:feature:voice` | voice record/playback/transcription |
| `:feature:expenses` | group expenses, splits, balances and settlements |
| `:feature:payment` | settle-up payments with bank details and SEPA payment QR codes |
| `:feature:ocr` | on-device receipt text recognition for expenses |
| `:feature:invite` | invitation lifecycle/inbox |
| `:feature:identity` | local/remote identity, trust, backup/recovery |
| `:feature:membership` | group membership/security/admin lifecycle |
| `:feature:conversationorchestration:*` | every conversation write and incoming packet as workflow state machines (`api`, `engine`, `delivery`, `message`, `groupstate`, `membership`, `lifecycle`, `incoming`) |
| `:feature:messaging` | durable outbox/incoming-envelope execution |
| `:feature:transport` | discovery/WebSocket/presence/mailbox/push transport |
| `:feature:applock` | application-lock setting/authentication boundary |
| `:feature:notification` | app notifications/background synchronization |

## Startup/runtime split

Application startup no longer lives as one large `AppViewModel` procedure. `ApplicationStartupRunner` consumes a list of `StartupTask`s. Waiting tasks complete before main navigation; background tasks and `ForegroundRuntimeCoordinator` start after `StartupViewModel.completeStartup()` navigates to `AppRoute.Main`.

See [Startup architecture](startup.md).

## Feature-internal layering

```mermaid
flowchart TD
    P[Presentation / *Ui] --> U[Domain use cases]
    U --> R[Domain repository contracts]
    RI[Data repository implementation] --> R
    RI --> DS[Datasource / persistence / network adapter]
    DS --> EXT[(Room / DataStore / Ktor / platform API)]
```

Repositories do not call other repositories or use cases; datasources do not call repositories. Cross-feature workflows belong in the [conversation orchestration](conversation-orchestration.md) workflows. Presentation receives presentation models rather than DTOs/entities.

## Direct vs Group

Direct and Group remain separate where authorization, recipients, membership, delivery aggregation and group epoch/admin semantics differ. They share `MessagePart`, `OperationMessage`, attachment infrastructure and durable transport execution.

## Persistence

Room schema is version **55**. Structured message-part payloads are stored in `MessagePartEntity.payload`; blob metadata/cache paths are in `MessageBlobEntity`. See [Persistence](persistence.md).

## Server topology

The Control Plane owns discovery/presence/push control functions. Community Nodes own client gateway, federation, mailbox and encrypted blob transport. Server details are documented under [Server & Operations](../server/overview.md).
