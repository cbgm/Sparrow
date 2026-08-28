# Roadmap

This is direction, not a release promise.

## Current usable baseline

Android currently has the core Sparrow experience: identity/onboarding, contacts/invitations/verification, Direct and Group chats, typed text/attachment message parts, encrypted image/video/file/location/contact attachments, attachment storage management, delivery/read/typing, optional local semantic search, optional local message-safety analysis, Control Plane discovery/failover, Community Node federation/mailbox delivery, Android notifications, Docker launchers and release automation.

## Near-term priorities

- stabilize current Direct/Group and attachment behavior with regression/integration tests;
- continue improving attachment UX/performance and large-media transfer robustness;
- improve semantic-search index lifecycle and message-safety evaluation coverage;
- publish the first official pre-release package through the existing `v*` workflow;
- harden server operations/backup/upgrade paths;
- continue security review/protocol hardening;
- improve release smoke tests for minified Android builds and real multi-node deployment.

## Client features not yet considered complete

- iOS platform/runtime implementation and feature parity;
- multi-device identity/synchronization;
- message reactions/replies/editing/deletion if introduced;
- desktop client packaging;
- voice/video calling.

## Longer-term security work

Potential work includes stronger post-compromise properties for group messaging, encrypted backup/recovery designs, additional automated dependency/security scanning and independent security review.

Any such change must preserve explicit protocol/versioning and the current separation between client message semantics and server routing infrastructure.
