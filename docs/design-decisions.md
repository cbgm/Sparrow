# Design decisions

## One shared message-part hierarchy

Message content is represented by `MessagePartDto` -> `MessagePart` -> `MessagePartUi` under `:core:base`. Image, video, file, voice, location, contact and poll are concrete variants of the same hierarchy.

Reason: chat, attachments, voice, polls, pinned messages and protocol serialization need the same content semantics; parallel feature-owned hierarchies caused duplicate mapping and nullable/type-field workarounds.

## Attachment module owns bytes, not message semantics

`:feature:attachments` owns encrypted blob transfer, cache, saved copies and attachment loading. It does not own a second generic `MessageAttachment` content hierarchy. Chat/poll/voice content remains a shared `MessagePart`.

## General operation messages

`OperationMessage` carries one `MessageOperation`: `Edit`, `Delete`, `Reaction`, `PollVote` or `PollClose`. The operation owns the target `messageId`; the envelope/packet ID remains transport metadata.

Reason: edit/delete/reaction/poll updates are operations on an existing message, not new user-message content.

## Poll expiry is derived, manual close is synchronized

Poll creation stores an absolute `expiresAtEpochMilliseconds` derived from an input duration in minutes. `PollPolicy.isClosedAt(...)` treats expiry as closed when evaluated. Expiry does not broadcast a `PollClose`; only an explicit creator/admin close writes `closedAtEpochMilliseconds` and sends `MessageOperation.PollClose`.

Reason: client clock skew should not let one device permanently broadcast an early automatic close. The absolute timestamp still makes every client converge on closed as its clock passes the same deadline.

## Poll creation hands off to chat sending

`:feature:polls` finishes a domain `Poll` into `PollComposerRepository`. `GroupConversationViewModel` observes and sends it through the existing group-message orchestration.

Reason: polls are group message parts. The poll UI should not own a parallel group send stack.

## Pin is a source context, not a separate attachment type

Pinned snapshots can load detached message parts, including nested poll images, but they reuse the generic attachment/blob cache. No `writePinned`/`readPinned` filesystem API is required.

## Task-based startup

Application startup is a list of `StartupTask` instances. Only the small set marked `waitForCompletion` blocks navigation; background runtime starts after `AppRoute.Main`.

Reason: startup has one visible red line, avoids a bloated startup ViewModel, and keeps application-lifetime observers out of Composable lifecycle.

## Strict feature dependency direction

Datasources do not call repositories; repository implementations do not call repositories/use cases. Cross-feature business workflows live in explicit orchestration/runtime boundaries such as `:feature:conversationorchestration`.

## Direct and Group remain semantically separate

Direct and Group processors/ViewModels/repositories remain separate where membership/security/delivery semantics differ. They share only genuinely common primitives such as message parts, protocol operations and transport infrastructure.

## Durable outbox before transport

Packet-producing features enqueue to the durable protocol outbox; messaging/transport runtime processes it later. A screen does not need to stay open for delivery/retry.

## App lock uses platform authentication behind a common contract

`:feature:applock` keeps enabled state/domain/presentation in common code and delegates device-owner authentication to expect/actual `AppLockAuthenticationLauncher` implementations.
