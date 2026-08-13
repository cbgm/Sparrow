# SecureChat Roadmap

## Overview

This roadmap outlines the planned evolution of SecureChat.

It serves as a high-level planning document rather than a strict release schedule.

Priorities may change as development progresses, but the roadmap communicates the intended direction of the project.

---

# Guiding Principles

Every planned feature should

- improve privacy
- preserve end-to-end encryption
- maintain Clean Architecture
- remain multiplatform where practical
- avoid unnecessary complexity

New functionality should integrate naturally with the existing architecture.

---

# Current Status

The current implementation provides the foundation of SecureChat.

Completed areas include

- Kotlin Multiplatform architecture
- Modular project structure
- Clean Architecture
- Identity management
- Contact management
- Secure messaging
- End-to-end encryption
- Gateway service
- Documentation generation
- Architecture validation
- Quality automation
- Centralized multiplatform logging

---

# Version 1.0

## Core Messaging

- Stable encrypted messaging
- Contact management
- Identity sharing
- Safety Number verification
- Reliable transport
- Offline message queue
- Delivery states

Goal

A secure one-to-one messaging platform suitable for daily use.

---

# Version 1.1

## Attachments

Planned features

- Image sharing
- Video sharing
- File transfer
- Attachment previews
- Attachment encryption

Attachments should use exactly the same encryption pipeline as text messages.

---

# Version 1.2

## Groups

Implemented foundation

- Group creation
- Signed invitation and join-request identity bootstrap for ordinary contacts
- Independent per-member activation after explicit acceptance
- Shared XChaCha20-Poly1305 epoch key
- Signed per-sender group messages
- Signed, recipient-wrapped epoch-1 key distribution
- Android Keystore protection for local group keys
- Owner group administration inside group details
- Invitations for adding members to an active group
- Epoch rotation when adding or removing an active member
- Signed cancellation/removal notification with local key deletion

Remaining functionality

- Multi-admin roles and ownership transfer
- Sender-key or ratcheting research if stronger post-compromise properties are required

Architecture should remain compatible with one-to-one messaging.

---

# Version 1.3

## Message Improvements

Possible additions

- Reactions
- Replies
- Message editing
- Message deletion
- Rich previews

These features should not compromise protocol simplicity.

---

# Version 1.4

## Multi-Device

Long-term goal

Support multiple trusted devices for a single identity.

Possible functionality

- device registration
- encrypted synchronization
- trusted device management
- device revocation

This feature requires significant protocol evolution.

---

# Version 1.5

## Desktop Support

Potential targets

- Windows
- macOS
- Linux

Because the project is Kotlin Multiplatform, most business logic should already be reusable.

---

# Version 2.0

## Voice & Video

Possible future functionality

- Voice calls
- Video calls
- Secure signaling
- Screen sharing

This work should build on the existing transport and identity infrastructure.

---

# Security Roadmap

Future security improvements may include

- encrypted backups
- hardware-backed key storage improvements
- post-quantum cryptography research
- additional protocol hardening
- transparency logging

Security improvements remain an ongoing effort rather than a single milestone.

---

# Developer Experience

Future build improvements

- interactive architecture explorer
- dependency dashboard
- build performance reports
- automated dependency updates
- additional custom Detekt rules

The goal is to continuously improve development efficiency.

---

# Documentation

Future documentation improvements

- interactive diagrams
- searchable API reference
- protocol specification
- sequence diagrams
- architecture decision records

Documentation should evolve together with the codebase.

---

# Performance

Areas of ongoing optimization

- startup time
- Compose rendering
- database performance
- synchronization
- memory usage
- build speed

Performance work should remain measurable.

---

# Quality

Planned improvements

- additional architecture rules
- expanded unit test coverage
- mutation testing
- dependency vulnerability scanning
- license verification

Quality automation should continue to grow alongside the application.

---

# Long-Term Vision

The long-term goal of SecureChat is to become

- privacy-first
- open
- modular
- maintainable
- cross-platform
- independently auditable

Every major architectural decision should move the project closer to these goals.

---

# Summary

The roadmap describes the intended evolution of SecureChat from a secure one-to-one messaging application into a mature, cross-platform communication platform while preserving its core principles of privacy, simplicity and strong engineering practices.
