# Roadmap

This is direction, not a release promise.

## Current usable baseline

Android currently has the core Sparrow experience plus the newer separated Invite/Membership/Conversation-Orchestration architecture, encrypted identity backup/restore and explicit identity-reconnection state, voice attachments with local transcription, link previews, auto-reply, Group pin state, avatar editing, encrypted attachment storage, local search/safety, signed discovery/failover, mailbox/federation delivery and unified server tooling.

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
- desktop client packaging;
- voice/video calling.

## Longer-term security work

Potential work includes stronger post-compromise properties for group messaging, broader backup beyond the already implemented encrypted identity-key backup/restore, secure recovery for a compromised independent-directory signing key, additional automated dependency/security scanning and independent security review.

Any such change must preserve explicit protocol/versioning and the current separation between client message semantics and server routing infrastructure.
