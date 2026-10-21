# Documentation audit

Audit basis: the source tree on **2026-10-21**.

## Structural findings

- `58` Gradle modules are included by `settings.gradle.kts`, including `:feature:expenses`, `:feature:payment`, `:feature:ocr` and `:server:exchange-rates`.
- `:feature:polls`, `:feature:applock`, `:feature:expenses` and `:feature:payment` are first-class feature modules and are documented in the hand-written feature pages ([Group expenses](../features/expenses.md), [Settle up and payment](../features/payment.md)).
- Shared message-part models live under `:core:base` (`com.cbgm.sparrow.core.base.messagepart`). The packages of `:core:base` and `:core:util` follow their modules: `com.cbgm.sparrow.core.base.*` and `com.cbgm.sparrow.core.util.*`.
- Message parts include the expense board, expense, closed board and payment transfer parts next to text, media, files, voice, location, contact and poll parts; every part type has a fixed `@SerialName`.
- Protocol operations are unified by `OperationMessage` / `MessageOperation` for edit, delete, reaction, poll vote and poll close.
- Startup orchestration is centered on `StartupRunner` (`ApplicationStartupRunner`) and `StartupViewModel`; the startup UI is only a readiness/error/identity gate.
- Sign-up verifies the phone number once (Firebase SMS) for both creating and restoring an identity; the one-time token is kept in memory only and sent with the first route registration ([Identity](../features/identity.md#phone-verification-at-sign-up)).
- Room message persistence uses `MessagePartEntity`, `MessageBlobEntity` and `MessageTextEntity` plus reactions, recipient state and conversation/group tables. `MessageStructuredEntity.kt` remains only as legacy source. The schema is at version 56 (`parked_message_operations`, auto-migration); `MessagePartPayloadMigration53To54` is the only registered manual migration.

## Generated-reference note

`docs/generated/` was regenerated with `./gradlew architectureReport`. This inventory was generated from the git-tracked Kotlin sources (build output such as `.cxx` is no longer included). No claim of a successful full Gradle build of every module is made.
