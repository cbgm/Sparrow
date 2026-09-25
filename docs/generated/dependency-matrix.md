# Dependency matrix

| Module | Direct project dependencies |
|---|---|
| `:androidApp` | `:shared` |
| `:shared` | `:core`, `:data:datastore`, `:core:embedding`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:navigation`, `:feature:autoreply`, `:feature:avatar`, `:feature:chats`, `:feature:attachments`, `:feature:contactimport`, `:feature:contacts`, `:feature:conversationorchestration`, `:feature:invite`, `:feature:identity`, `:feature:media`, `:feature:membership`, `:feature:voice`, `:feature:linkpreview`, `:feature:messaging`, `:feature:onboarding`, `:feature:settings`, `:feature:search`, `:feature:safety`, `:feature:transport`, `:notification`, `:startup`, `:data:database` |
| `:core` | — |
| `:data:datastore` | — |
| `:core:embedding` | `:core`, `:data:datastore` |
| `:feature:identity` | `:core`, `:data:datastore`, `:data:database`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:feature:avatar` |
| `:feature:invite` | `:core`, `:core:ui`, `:feature:avatar`, `:data:database` |
| `:feature:contacts` | `:core`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:feature:avatar`, `:feature:identity`, `:feature:transport`, `:data:database` |
| `:data:database` | `:core`, `:core:protocol` |
| `:feature:contactimport` | `:core`, `:core:ui`, `:feature:contacts`, `:feature:identity`, `:feature:invite`, `:data:database`, `:core:protocol` |
| `:feature:autoreply` | `:core`, `:core:ui`, `:data:database` |
| `:feature:avatar` | `:core`, `:core:protocol`, `:core:ui`, `:feature:media` |
| `:feature:linkpreview` | `:core`, `:core:ui`, `:data:database` |
| `:feature:chats` | `:core`, `:data:datastore`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:feature:avatar`, `:data:database`, `:feature:autoreply`, `:feature:contactimport`, `:feature:contacts`, `:feature:conversationorchestration`, `:feature:attachments`, `:feature:identity`, `:feature:media`, `:feature:membership`, `:feature:voice`, `:feature:linkpreview`, `:feature:safety`, `:feature:transport` |
| `:feature:conversationorchestration` | `:core`, `:core:crypto`, `:core:protocol`, `:feature:contacts`, `:feature:identity`, `:feature:invite`, `:feature:membership`, `:feature:messaging`, `:feature:transport` |
| `:feature:membership` | `:core`, `:core:crypto`, `:core:protocol`, `:data:database`, `:data:datastore` |
| `:feature:attachments` | `:core`, `:core:crypto`, `:core:protocol`, `:core:ui`, `:data:database`, `:feature:media`, `:feature:transport` |
| `:feature:media` | `:core`, `:core:ui` |
| `:feature:voice` | `:core`, `:core:protocol`, `:core:ui`, `:data:datastore`, `:feature:attachments` |
| `:core:crypto` | — |
| `:core:protocol` | `:core`, `:core:crypto` |
| `:feature:messaging` | `:core`, `:core:protocol` |
| `:feature:transport` | `:core`, `:data:datastore`, `:core:crypto`, `:core:protocol` |
| `:feature:onboarding` | `:core:ui`, `:feature:identity`, `:feature:media` |
| `:startup` | `:core`, `:core:ui`, `:core:embedding`, `:feature:identity`, `:feature:onboarding`, `:feature:search`, `:feature:safety`, `:feature:transport` |
| `:navigation` | `:core`, `:core:ui`, `:feature:attachments`, `:feature:autoreply`, `:feature:chats`, `:feature:contactimport`, `:feature:contacts`, `:feature:conversationorchestration`, `:feature:identity`, `:feature:invite`, `:feature:membership`, `:feature:media`, `:feature:onboarding`, `:feature:settings`, `:feature:search`, `:feature:safety`, `:notification`, `:startup` |
| `:notification` | `:core`, `:core:crypto`, `:feature:chats`, `:feature:messaging`, `:feature:transport`, `:core:protocol`, `:resources` |
| `:core:ui` | `:resources`, `:core` |
| `:feature:settings` | `:core`, `:data:datastore`, `:core:embedding`, `:core:ui`, `:feature:identity`, `:feature:contacts`, `:feature:avatar`, `:feature:autoreply`, `:feature:voice`, `:feature:search`, `:feature:safety` |
| `:feature:search` | `:core`, `:core:embedding`, `:core:ui`, `:data:database` |
| `:feature:safety` | `:core`, `:core:embedding`, `:core:ui`, `:data:database`, `:feature:contacts` |
| `:quality:detekt-rules` | — |
| `:resources` | — |
| `:server:protocol` | — |
| `:server:security` | `:server:protocol` |
| `:server:persistence` | `:server:protocol` |
| `:server:observability` | — |
| `:server:node-registry` | `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability` |
| `:server:presence-directory` | `:server:persistence`, `:server:protocol`, `:server:security`, `:server:observability` |
| `:server:gateway` | `:server:persistence`, `:server:protocol`, `:server:security`, `:server:observability`, `:server:linkPreview` |
| `:server:federation` | `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability` |
| `:server:mailbox` | `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability` |
| `:server:push` | `:server:protocol`, `:server:persistence`, `:server:security`, `:server:observability` |
| `:server:link-preview` | `:server:protocol` |
