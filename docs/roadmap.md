# Roadmap

This is direction, not a release promise.

## Current usable baseline

Android currently has the core Sparrow experience: identity/onboarding, contacts/invitations/verification, Direct chats, Group chats/membership/admin/security, delivery/read/typing, Control Plane discovery/failover, Community Node federation/mailbox delivery, Android notifications, Docker launchers and release automation.

## Near-term priorities

- stabilize current Direct/Group behavior with regression/integration tests;
- publish the first official pre-release package through the existing `v*` workflow;
- harden server operations/backup/upgrade paths;
- continue security review/protocol hardening;
- improve release smoke tests for minified Android builds and real multi-node deployment.

## Client features not yet considered complete

- iOS platform/runtime implementation and feature parity;
- attachments/media/file transfer;
- multi-device identity/synchronization;
- message reactions/replies/editing/deletion if introduced;
- desktop client packaging.

## Longer-term security work

Potential work includes stronger post-compromise properties for group messaging, encrypted backup/recovery designs, additional automated dependency/security scanning and independent security review.

Any such change must preserve explicit protocol/versioning and the current separation between client message semantics and server routing infrastructure.
