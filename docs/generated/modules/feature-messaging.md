# `:feature:messaging`

Source directory: `feature/messaging`

## Direct project dependencies

- `:core`
- `:core:protocol`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `MessagingFailureEvent` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingFailureEvent.kt` |
| `MessagingIndicator` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingIndicator.kt` |
| `MessagingTransportResult` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingTransportResult.kt` |
| `MessagingTransportState` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/model/MessagingTransportResult.kt` |
| `AcknowledgeMessagingFailureUseCase` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/AcknowledgeMessagingFailureUseCase.kt` |
| `ObserveMessagingFailureEventsUseCase` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingFailureEventsUseCase.kt` |
| `ObserveMessagingIndicatorsUseCase` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingIndicatorsUseCase.kt` |
| `ObserveMessagingTransportResultsUseCase` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/ObserveMessagingTransportResultsUseCase.kt` |
| `SendEncodedTransportUseCase` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/SendEncodedTransportUseCase.kt` |
| `SendMessagingIndicatorUseCase` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/domain/usecase/SendMessagingIndicatorUseCase.kt` |
| `DefaultIncomingEnvelopeRunner` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/DefaultIncomingEnvelopeRunner.kt` |
| `IncomingEnvelopeGateway` | `interface` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeGateway.kt` |
| `IncomingEnvelopeProcessingResult` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeProcessor.kt` |
| `IncomingEnvelopeProcessor` | `interface` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeProcessor.kt` |
| `IncomingEnvelopeRunner` | `interface` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeRunner.kt` |
| `IncomingTransportEnvelope` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/incoming/IncomingEnvelopeGateway.kt` |
| `MessagingIndicatorGateway` | `interface` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/indicator/MessagingIndicatorGateway.kt` |
| `MailboxCoordinator` | `interface` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/mailbox/MailboxCoordinator.kt` |
| `MailboxRoutePayloadEncoder` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/mailbox/MailboxRoutePayloadEncoder.kt` |
| `DefaultOutboxProcessor` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxProcessor.kt` |
| `DefaultOutboxRunner` | `class` | `commonMain` | `feature/messaging/src/commonMain/kotlin/com/cbgm/sparrow/feature/messaging/runtime/outbox/DefaultOutboxRunner.kt` |
