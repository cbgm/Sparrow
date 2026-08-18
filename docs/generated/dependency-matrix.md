# Dependency Matrix

A check mark means the row module directly depends on the column module.

| Module | `androidApp` | `core` | `core/crypto` | `core/datastore` | `core/protocol` | `core/ui` | `data` | `data/database` | `feature` | `feature/chats` | `feature/contactimport` | `feature/contacts` | `feature/identity` | `feature/messaging` | `feature/onboarding` | `feature/settings` | `feature/transport` | `navigation` | `notification` | `quality` | `quality/detekt-rules` | `resources` | `server` | `server/federation` | `server/gateway` | `server/mailbox` | `server/node-registry` | `server/observability` | `server/persistence` | `server/presence-directory` | `server/protocol` | `server/push` | `server/security` | `shared` | `startup` |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `:androidApp` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ |  |
| `:core` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:core:crypto` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:core:datastore` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:core:protocol` |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:core:ui` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:data` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:data:database` |  | ✓ |  |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:chats` |  | ✓ | ✓ | ✓ | ✓ | ✓ |  | ✓ |  |  | ✓ | ✓ | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:contactimport` |  | ✓ |  |  |  | ✓ |  |  |  |  |  | ✓ | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:contacts` |  | ✓ | ✓ |  | ✓ | ✓ |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:identity` |  | ✓ | ✓ | ✓ | ✓ | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:messaging` |  | ✓ | ✓ |  | ✓ |  |  | ✓ |  | ✓ |  | ✓ |  |  |  |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:onboarding` |  |  |  |  |  | ✓ |  |  |  |  |  |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:settings` |  | ✓ |  | ✓ |  | ✓ |  |  |  |  |  |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:transport` |  | ✓ | ✓ | ✓ | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:navigation` |  | ✓ |  |  |  | ✓ |  |  |  | ✓ | ✓ | ✓ | ✓ |  | ✓ | ✓ |  |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ |
| `:notification` |  | ✓ | ✓ |  |  |  |  |  |  | ✓ |  |  |  | ✓ |  |  | ✓ |  |  |  |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:quality` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:quality:detekt-rules` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:resources` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:server` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:server:federation` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ | ✓ |  | ✓ |  | ✓ |  |  |
| `:server:gateway` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ | ✓ |  | ✓ |  | ✓ |  |  |
| `:server:mailbox` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ | ✓ |  | ✓ |  | ✓ |  |  |
| `:server:node-registry` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ | ✓ |  | ✓ |  | ✓ |  |  |
| `:server:observability` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:server:persistence` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ |  |  |  |  |
| `:server:presence-directory` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ | ✓ |  | ✓ |  | ✓ |  |  |
| `:server:protocol` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:server:push` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ | ✓ |  | ✓ |  | ✓ |  |  |
| `:server:security` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ |  |  |  |  |
| `:shared` |  | ✓ | ✓ | ✓ | ✓ | ✓ |  | ✓ |  | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | ✓ |
| `:startup` |  |  |  |  |  | ✓ |  |  |  |  |  |  | ✓ |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
