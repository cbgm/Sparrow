# `:notification`

Source directory: `notification`

## Direct project dependencies

- `:core`
- `:core:crypto`
- `:feature:chats`
- `:feature:messaging`
- `:feature:transport`
- `:core:protocol`
- `:resources`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidNotificationRuntime` | `class` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/AndroidNotificationRuntime.kt` |
| `BackgroundDeliveryReceiptSender` | `class` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/BackgroundDeliveryReceiptSender.kt` |
| `PendingMessageSyncScheduler` | `class` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/PendingMessageSyncScheduler.kt` |
| `PendingMessageSyncWorker` | `class` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/PendingMessageSyncWorker.kt` |
| `PushTokenRegistrationScheduler` | `class` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/PushTokenRegistrationScheduler.kt` |
| `PushTokenRegistrationWorker` | `class` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/PushTokenRegistrationWorker.kt` |
| `SparrowDeepLink` | `object` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowDeepLink.kt` |
| `SparrowFirebaseMessagingService` | `class` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowFirebaseMessagingService.kt` |
| `SparrowNotificationIntentFactory` | `object` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowNotificationIntentFactory.kt` |
| `SparrowNotificationIntentHandler` | `class` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowNotificationIntentHandler.kt` |
| `SparrowNotificationManager` | `class` | `androidMain` | `notification/src/androidMain/kotlin/com/cbgm/sparrow/notification/device/SparrowNotificationManager.kt` |
| `PlatformNotificationRuntime` | `interface` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/device/PlatformNotificationRuntime.kt` |
| `AppVisibilityState` | `class` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/AppVisibilityState.kt` |
| `ConversationNotification` | `class` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/ConversationNotification.kt` |
| `ConversationNotificationEvent` | `interface` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/ConversationNotificationEvent.kt` |
| `NotificationConversationTarget` | `interface` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/NotificationConversationTarget.kt` |
| `PendingMessageSyncResult` | `class` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/model/PendingMessageSyncResult.kt` |
| `ObserveConversationNotificationEventsUseCase` | `class` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/usecase/ObserveConversationNotificationEventsUseCase.kt` |
| `RegisterPushTokenUseCase` | `class` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/usecase/RegisterPushTokenUseCase.kt` |
| `ResolveNotificationConversationUseCase` | `class` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/usecase/ResolveNotificationConversationUseCase.kt` |
| `SynchronizePendingMessagesUseCase` | `class` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/domain/usecase/SynchronizePendingMessagesUseCase.kt` |
| `ConversationNotificationCoordinator` | `class` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/presentation/ConversationNotificationCoordinator.kt` |
| `ConversationNotificationPresenter` | `interface` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/presentation/ConversationNotificationPresenter.kt` |
| `NotificationNavigationController` | `class` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/presentation/navigation/NotificationNavigationController.kt` |
| `NotificationNavigationTarget` | `interface` | `commonMain` | `notification/src/commonMain/kotlin/com/cbgm/sparrow/notification/presentation/navigation/NotificationNavigationTarget.kt` |
