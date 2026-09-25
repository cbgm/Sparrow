# `:shared`

Source directory: `shared`

## Direct project dependencies

- `:core`
- `:data:datastore`
- `:core:embedding`
- `:core:crypto`
- `:core:protocol`
- `:core:ui`
- `:navigation`
- `:feature:autoreply`
- `:feature:avatar`
- `:feature:chats`
- `:feature:attachments`
- `:feature:contactimport`
- `:feature:contacts`
- `:feature:conversationorchestration`
- `:feature:invite`
- `:feature:identity`
- `:feature:media`
- `:feature:membership`
- `:feature:voice`
- `:feature:linkpreview`
- `:feature:messaging`
- `:feature:onboarding`
- `:feature:settings`
- `:feature:search`
- `:feature:safety`
- `:feature:transport`
- `:notification`
- `:startup`
- `:data:database`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidPlatform` | `class` | `androidMain` | `shared/src/androidMain/kotlin/com/cbgm/sparrow/device/Platform.android.kt` |
| `Platform` | `interface` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/device/Platform.kt` |
| `AppViewModel` | `class` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/presentation/AppViewModel.kt` |
| `AppInitializationDependencies` | `class` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/presentation/model/AppInitializationDependencies.kt` |
| `ForegroundRuntimeDependencies` | `class` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/presentation/model/ForegroundRuntimeDependencies.kt` |
| `AttachmentConversationNameObserver` | `class` | `commonMain` | `shared/src/commonMain/kotlin/com/cbgm/sparrow/runtime/AttachmentConversationNameObserver.kt` |
| `IOSPlatform` | `class` | `iosMain` | `shared/src/iosMain/kotlin/com/cbgm/sparrow/device/Platform.ios.kt` |
