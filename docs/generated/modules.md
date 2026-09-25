# Module catalog

The current build declares **44 Gradle modules**. This catalog was refreshed from `settings.gradle.kts` and each module build file.

| Module | Path | Direct project dependencies | Production declarations |
|---|---|---|---:|
| `:androidApp` | `androidApp` | `:shared` | 2 |
| `:shared` | `shared` | `:core`, `:data:datastore`, `:core:embedding`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:navigation`, `:feature:autoreply`, `:feature:avatar`, `:feature:chats`, `:feature:attachments`, `:feature:contactimport`, `:feature:contacts`, `:feature:conversationorchestration`, `:feature:invite`, `:feature:identity`, `:feature:media`, `:feature:membership`, `:feature:voice`, `:feature:linkpreview`, `:feature:messaging`, `:feature:onboarding`, `:feature:settings`, `:feature:search`, `:feature:safety`, `:feature:transport`, `:notification`, `:startup`, `:data:database` | 7 |
| `:core` | `core` | — | 20 |
| `:data:datastore` | `data/datastore` | — | 2 |
| `:core:embedding` | `core/embedding` | `:core`, `:data:datastore` | 18 |
| `:feature:identity` | `feature/identity` | `:core`, `:data:datastore`, `:data:database`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:feature:avatar` | 155 |
| `:feature:invite` | `feature/invite` | `:core`, `:core:ui`, `:feature:avatar`, `:data:database` | 43 |
| `:feature:contacts` | `feature/contacts` | `:core`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:feature:avatar`, `:feature:identity`, `:feature:transport`, `:data:database` | 88 |
| `:data:database` | `data/database` | `:core`, `:core:protocol` | 80 |
| `:feature:contactimport` | `feature/contactimport` | `:core`, `:core:ui`, `:feature:contacts`, `:feature:identity`, `:feature:invite`, `:data:database`, `:core:protocol` | 13 |
| `:feature:autoreply` | `feature/autoreply` | `:core`, `:core:ui`, `:data:database` | 19 |
| `:feature:avatar` | `feature/avatar` | `:core`, `:core:protocol`, `:core:ui`, `:feature:media` | 28 |
| `:feature:linkpreview` | `feature/linkpreview` | `:core`, `:core:ui`, `:data:database` | 14 |
| `:feature:chats` | `feature/chats` | `:core`, `:data:datastore`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:feature:avatar`, `:data:database`, `:feature:autoreply`, `:feature:contactimport`, `:feature:contacts`, `:feature:conversationorchestration`, `:feature:attachments`, `:feature:identity`, `:feature:media`, `:feature:membership`, `:feature:voice`, `:feature:linkpreview`, `:feature:safety`, `:feature:transport` | 283 |
| `:feature:conversationorchestration` | `feature/conversationorchestration` | `:core`, `:core:crypto`, `:core:protocol`, `:feature:contacts`, `:feature:identity`, `:feature:invite`, `:feature:membership`, `:feature:messaging`, `:feature:transport` | 57 |
| `:feature:membership` | `feature/membership` | `:core`, `:core:crypto`, `:core:protocol`, `:data:database`, `:data:datastore` | 125 |
| `:feature:attachments` | `feature/attachments` | `:core`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:data:database`, `:feature:media`, `:feature:transport` | 66 |
| `:feature:media` | `feature/media` | `:core`, `:core:ui` | 66 |
| `:feature:voice` | `feature/voice` | `:core`, `:core:protocol`, `:core:ui`, `:data:datastore`, `:feature:attachments` | 51 |
| `:core:crypto` | `core/crypto` | — | 43 |
| `:core:protocol` | `core/protocol` | `:core`, `:core:crypto` | 109 |
| `:feature:messaging` | `feature/messaging` | `:core`, `:core:protocol` | 21 |
| `:feature:transport` | `feature/transport` | `:core`, `:data:datastore`, `:core:crypto`, `:core:protocol` | 89 |
| `:feature:onboarding` | `feature/onboarding` | `:core:ui`, `:feature:identity`, `:feature:media` | 7 |
| `:startup` | `startup` | `:core`, `:core:ui`, `:core:embedding`, `:feature:identity`, `:feature:onboarding`, `:feature:search`, `:feature:safety`, `:feature:transport` | 9 |
| `:navigation` | `navigation` | `:core`, `:core:ui`, `:feature:attachments`, `:feature:autoreply`, `:feature:chats`, `:feature:contactimport`, `:feature:contacts`, `:feature:conversationorchestration`, `:feature:identity`, `:feature:invite`, `:feature:membership`, `:feature:media`, `:feature:onboarding`, `:feature:settings`, `:feature:search`, `:feature:safety`, `:notification`, `:startup` | 5 |
| `:notification` | `notification` | `:core`, `:core:crypto`, `:feature:chats`, `:feature:messaging`, `:feature:transport`, `:core:protocol`, `:resources` | 25 |
| `:core:ui` | `core/ui` | `:resources`, `:core` | 44 |
| `:feature:settings` | `feature/settings` | `:core`, `:data:datastore`, `:core:embedding`, `:core:ui`, `:feature:identity`, `:feature:contacts`, `:feature:avatar`, `:feature:autoreply`, `:feature:voice`, `:feature:search`, `:feature:safety` | 67 |
| `:feature:search` | `feature/search` | `:core`, `:core:embedding`, `:core:ui`, `:data:database` | 22 |
| `:feature:safety` | `feature/safety` | `:core`, `:core:embedding`, `:core:ui`, `:data:database`, `:feature:contacts` | 24 |
| `:quality:detekt-rules` | `quality/detekt-rules` | — | 10 |
| `:resources` | `resources` | — | 0 |
| `:server:protocol` | `server/protocol` | — | 35 |
| `:server:security` | `server/security` | `:server:protocol` | 22 |
| `:server:persistence` | `server/persistence` | `:server:protocol` | 3 |
| `:server:observability` | `server/observability` | — | 1 |
| `:server:node-registry` | `server/node-registry` | `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability` | 15 |
| `:server:presence-directory` | `server/presence-directory` | `:server:persistence`, `:server:protocol`, `:server:security`, `:server:observability` | 7 |
| `:server:gateway` | `server/gateway` | `:server:persistence`, `:server:protocol`, `:server:security`, `:server:observability`, `:server:linkPreview` | 32 |
| `:server:federation` | `server/federation` | `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability` | 21 |
| `:server:mailbox` | `server/mailbox` | `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability` | 14 |
| `:server:push` | `server/push` | `:server:protocol`, `:server:persistence`, `:server:security`, `:server:observability` | 18 |
| `:server:link-preview` | `server/link-preview` | `:server:protocol` | 8 |
