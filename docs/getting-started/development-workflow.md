# Development Workflow

## Overview

This document describes the recommended day-to-day development workflow for SecureChat.

The workflow has been designed to minimize manual steps while ensuring that every commit satisfies the project's quality standards.

Most of the development process is automated through Gradle, Git hooks and Continuous Integration.

---

# Development Lifecycle

A typical feature follows this lifecycle.

```
Pull

↓

Develop

↓

quality

↓

Commit

↓

Push

↓

CI

↓

Merge
```

Every stage has a specific purpose.

---

# 1. Update the Repository

Before starting work, update the local branch.

```bash
git pull
```

If build logic changed, Gradle recompiles the convention plugins automatically during the next synchronization.

---

# 2. Create a Branch

Create a dedicated branch for the feature.

Example

```bash
git checkout -b feature/group-chat
```

Avoid working directly on the main branch.

---

# 3. Implement the Feature

Develop normally inside Android Studio.

SecureChat follows Clean Architecture.

Typical implementation order

```
Domain

↓

Data

↓

Presentation

↓

Navigation
```

Keeping this order reduces unnecessary refactoring.

---

# 4. Build Frequently

Compile the project regularly.

```bash
./gradlew build
```

Small incremental builds make issues easier to locate than waiting until the feature is complete.

---

# 5. Run Quality Checks

Before committing execute

```bash
./gradlew quality
```

This performs

1. Source formatting
2. Static analysis
3. Architecture validation
4. Documentation verification

Developers should treat quality failures as normal feedback rather than waiting for CI.

---

# Git Hooks

SecureChat uses tracked Git hooks.

The hooks are installed during

```bash
./gradlew setup
```

The repository contains

```
.githooks/
```

instead of relying on Git's local hook directory.

---

# Pre-Commit Hook

The pre-commit hook automatically formats Kotlin code.

```
Commit

↓

qualityFix

↓

Stage changes

↓

Commit again
```

If formatting modifies files the commit stops intentionally.

Developers should review the changes before committing them.

---

# Pre-Push Hook

The pre-push hook executes

```bash
./gradlew qualityCheck
```

This verifies

- KtLint
- Detekt
- Architecture
- Generated documentation

The push is rejected if verification fails.

---

# Documentation

Whenever the project structure changes execute

```bash
./gradlew architectureReport
```

This regenerates

- architecture documentation
- Mermaid diagrams
- module pages
- statistics
- dependency matrix

Generated documentation should be committed together with the architectural change.

---

# Commit Strategy

Prefer small focused commits.

Good examples

```
Add message reactions

Fix gateway reconnect

Refactor contact repository
```

Avoid commits containing unrelated changes.

---

# Code Reviews

Before opening a Pull Request verify

- project builds
- quality succeeds
- generated documentation is updated
- no temporary debugging code remains

The reviewer should be able to focus on the feature rather than formatting or architecture violations.

---

# Refactoring

Large refactorings should be split into separate commits.

Example

```
Commit 1

Move interfaces

Commit 2

Move implementations

Commit 3

Cleanup

Commit 4

Feature
```

This keeps Git history readable.

---

# Architecture Changes

Whenever a module dependency changes

```
feature

↓

data
```

or

```
shared

↓

navigation
```

run

```bash
./gradlew architectureReport
```

and review the generated documentation before committing.

---

# Updating Dependencies

Dependency versions are managed centrally through

```
gradle/libs.versions.toml
```

Do not hardcode versions inside module build files.

After updating dependencies run

```bash
./gradlew quality
```

to verify compatibility.

---

# Working with Convention Plugins

If changes are made inside

```
build-logic/
```

verify the complete project afterwards.

```bash
./gradlew build
```

Convention plugins affect every module and therefore require broader verification than normal feature work.

---

# Handling Build Failures

Always fix the first reported failure.

For example

```
build-logic

↓

Compilation

↓

Detekt

↓

Architecture

↓

Documentation
```

Resolving earlier failures often eliminates later ones.

---

# Continuous Integration

The CI pipeline executes the same verification steps used locally.

A Pull Request should therefore already pass

```bash
./gradlew quality
```

before it is pushed.

Keeping local and CI workflows identical reduces surprises.

---

# Pull Requests

Before creating a Pull Request ensure

- the branch is up to date
- the project builds
- quality passes
- generated documentation has been committed
- architecture changes have been reviewed

A Pull Request should represent a single logical change whenever possible.

---

# Daily Checklist

Before ending the workday

- Pull latest changes
- Resolve merge conflicts
- Run `quality`
- Commit completed work
- Push the branch

Following this routine keeps long-running branches healthy and reduces integration problems.

---

# Summary

The SecureChat workflow emphasizes automation.

Developers should spend their time implementing features rather than remembering build commands.

Gradle, Git hooks and CI work together to ensure

- consistent formatting
- clean architecture
- deterministic builds
- up-to-date documentation
- reliable verification

Following the workflow described in this guide results in a predictable and maintainable development process for the entire project.
