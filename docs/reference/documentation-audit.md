# Documentation audit

Audit basis: the supplied source archive dated **2026-10-08**.

## Structural findings

- `47` Gradle modules are included by `settings.gradle.kts`.
- `:feature:polls` and `:feature:applock` are first-class feature modules and are documented in the hand-written feature pages.
- Shared message-part models now live under `:core:base` (`core/messagepart`) instead of the older chat-owned `MessagePartDto` / `MessagePart` / `MessagePartUi` hierarchy.
- Protocol operations are unified by `OperationMessage` / `MessageOperation` for edit, delete, reaction, poll vote and poll close.
- Startup orchestration is centered on `StartupRunner` and `StartupViewModel`; the startup UI is only a readiness/error/identity gate.
- Room message persistence uses `MessagePartEntity`, `MessageBlobEntity` and `MessageTextEntity` plus reactions, recipient state and conversation/group tables. `MessageStructuredEntity.kt` remains only as legacy source; schema 54 stores structured message-part payload in `MessagePartEntity.payload` and `MessagePartPayloadMigration53To54` drops the old `message_structured` table.
- Polls are group-message parts with nested image parts, voting/close operations, optional duration-based expiry and pinned-message support.

## Generated-reference note

The checked environment did not have the required Gradle 9.6.1 distribution available, so this package regenerated `docs/generated/` directly from the source tree instead of executing `architectureReport`. No claim of a successful full Gradle build is made.
