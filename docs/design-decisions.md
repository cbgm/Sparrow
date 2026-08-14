# Design decisions

This page records the current architectural direction in plain language.

## Separate Direct and Group stacks

Direct and Group conversations share transport/protocol infrastructure only where semantics are truly identical. Membership, typing, delivery aggregation, message repositories, outgoing processing and UI/ViewModels remain separate.

Reason: Group membership/security/history semantics are materially different from one-to-one messaging, and forcing them through a generic chat abstraction previously made regressions easier to introduce.

## Persistent outbox before transport

Packet-producing features enqueue to `ProtocolOutbox`. `DefaultOutboxRunner`/`DefaultOutboxProcessor` handle later routing/encryption/wire transmission.

Reason: reliable retry and delivery state should survive temporary transport outages and should not require a screen to remain open.

## Control Plane directory instead of hardcoded plane URLs

The app and Community Node obtain Control Plane addresses from one configurable JSON directory. The app's build-time value is `BuildKonfig.CONTROL_PLANE_DIRECTORY_URL` sourced from `local.properties`.

Reason: plane addresses can change without embedding a list of deployment URLs in client/server source.

## Control Plane vs Community Node

The Control Plane owns discovery/presence/push. Community Nodes own client WebSockets, federation and mailbox storage.

Reason: routing capacity can scale independently and Community Nodes can be operated independently while using shared trusted discovery/control infrastructure.

## PostgreSQL for durable data, Redis for presence

Registry, push, federation queue and mailbox data require durable state and use PostgreSQL. Presence routes are short-lived and reconstructable, so Redis is a better fit.

## Caddy as the edge

Each deployable package exposes one operator/client edge and uses relative `/index` links. Internal JVM service names/ports remain Docker-internal details.

## Build-time directory variables

Normal/debug packages use GitHub variable `CONTROL_PLANE_DIRECTORY_URL`; signed release packages use `CONTROL_PLANE_RELEASE_DIRECTORY_URL`. Both produce the same KMP constant name inside their independent build.

## Incremental release candidates, full tagged releases

`release/**` pushes detect changed paths and only rebuild affected APK/images/bundles. A `v*` tag rebuilds the full application/server package so a published release is a complete reproducible snapshot.
