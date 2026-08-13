# Contributing

## Overview

This document describes the development standards expected when contributing to SecureChat.

Whether contributing a new feature, fixing a bug or improving documentation, every contribution should follow the same engineering principles.

The objective is to maintain a clean, secure and consistent codebase.

---

# Development Philosophy

SecureChat values

- simplicity
- readability
- maintainability
- security
- automation
- explicit architecture

When multiple solutions are possible, choose the one that is easiest to understand and maintain.

---

# Before You Start

Before implementing a change

- update your branch
- build the project
- ensure `quality` succeeds
- review the existing architecture

Avoid implementing features without understanding the surrounding module structure.

---

# Branches

Create a dedicated branch for every logical change.

Examples

```
feature/groups

feature/attachments

fix/message-order

refactor/contact-repository

docs/security
```

Avoid unrelated changes within the same branch.

---

# Small Commits

Prefer multiple focused commits instead of one large commit.

Example

```
Commit 1

Repository refactor

Commit 2

UseCase update

Commit 3

UI changes

Commit 4

Tests
```

Small commits simplify reviews and future debugging.

---

# Commit Messages

Write concise commit messages.

Good examples

```
Add message retry support

Refactor identity repository

Generate architecture statistics

Fix gateway reconnect logic
```

Avoid

```
fix

update

changes

misc
```

The commit message should describe **what** changed.

---

# Pull Requests

Every Pull Request should represent one logical change.

Before opening a Pull Request verify

- project builds
- tests pass
- quality passes
- architecture documentation is current

Large mixed Pull Requests are difficult to review.

---

# Code Reviews

Reviewers should focus on

- correctness
- architecture
- maintainability
- security
- readability

Formatting should already be handled automatically.

---

# Coding Standards

All code should follow the project coding style.

In particular

- meaningful names
- explicit dependencies
- immutable state where practical
- small functions
- small classes

Project formatting is enforced automatically.

---

# Architecture

Do not bypass architectural boundaries.

Presentation must not access

- DAO
- Room
- repository implementations

Domain should remain independent of infrastructure.

If a change requires breaking an architectural rule, reconsider the design before introducing exceptions.

---

# Dependencies

Before adding a dependency ask

- Is it really necessary?
- Can existing code solve the problem?
- Is it multiplatform?
- Is it actively maintained?
- Does it increase project complexity?

Every dependency becomes a long-term maintenance responsibility.

---

# Security

Security-sensitive changes deserve additional attention.

Examples include

- cryptography
- protocol changes
- identity management
- transport
- secure storage

Keep security-related Pull Requests as small as possible.

---

# Testing

New functionality should include appropriate tests.

Typical additions include

- unit tests
- integration tests
- regression tests

Bug fixes should generally include a regression test.

---

# Documentation

Update documentation whenever

- architecture changes
- module structure changes
- protocol changes
- public APIs change

Regenerate generated documentation with

```bash
./gradlew architectureReport
```

Generated documentation should never be edited manually.

---

# Quality Pipeline

Before committing execute

```bash
./gradlew quality
```

This performs

- formatting
- static analysis
- architecture validation
- documentation verification

A Pull Request should already satisfy these checks before CI executes.

---

# Generated Code

Never manually modify generated files.

Examples include

```
docs/generated/

build/generated/
```

Instead regenerate them using the appropriate Gradle task.

---

# Backwards Compatibility

When changing public APIs

- minimize breaking changes
- document migrations
- preserve compatibility where practical

Large API changes should be discussed before implementation.

---

# Performance

Performance improvements should

- be measurable
- preserve readability
- avoid unnecessary complexity

Avoid premature optimization.

---

# Issue Reporting

When reporting a bug include

- reproduction steps
- expected behaviour
- actual behaviour
- logs (if relevant)
- screenshots (if applicable)

A reproducible issue is significantly easier to resolve.

---

# Summary

SecureChat contributions should improve the project without compromising architecture, security or maintainability.

Following these guidelines keeps reviews efficient and ensures the project remains consistent as it grows.
