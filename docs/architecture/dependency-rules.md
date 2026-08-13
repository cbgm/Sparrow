# Dependency Rules

## Overview

SecureChat enforces architectural boundaries through both Gradle validation and custom Detekt rules.

These rules ensure that the dependency graph remains maintainable as the project grows.

Unlike traditional architecture documents, these rules are executable. Violations fail the build automatically.

---

# Design Goals

The dependency rules exist to ensure

- predictable dependencies
- reusable modules
- independent features
- platform independence
- long-term maintainability

Developers should not need to manually review every dependency change.

---

# Architectural Layers

The project is organized into the following layers.

```
Android App

↓

Shared

↓

Navigation

↓

Feature

↓

Data

↓

Core
```

Dependencies should always move downward.

---

# General Rules

Every module should satisfy the following principles.

- Dependencies are explicit.
- Dependencies should point toward more stable layers.
- Lower layers never depend on higher layers.
- Modules should have a single responsibility.
- Cyclic dependencies are forbidden.

---

# Core Modules

Core modules provide reusable infrastructure.

Examples

```
core:crypto

core:id

core:protocol

core:ui
```

Core modules

✅ may depend on other Core modules.

Core modules

❌ must never depend on

- Feature
- Navigation
- Shared
- Android application

This guarantees that Core remains reusable.

---

# Data Modules

Data modules provide implementations.

Examples

```
Room

Repositories

Persistence

Network
```

Data modules

✅ may depend on

- Core
- Domain abstractions

Data modules

❌ must never depend on

- Presentation
- Screens
- Routes
- ViewModels

Business decisions belong inside the Domain layer.

---

# Feature Modules

Feature modules implement user-visible functionality.

Examples

```
Chats

Contacts

Identity

Transport

Onboarding
```

Feature modules

✅ may depend on

- Core
- Data
- Shared

Feature-to-feature dependencies should be avoided whenever practical.

When they are necessary they should be minimal and based on stable public APIs.

---

# Shared Modules

Shared modules contain reusable application components.

Examples

- common UI
- reusable services
- shared utilities

Shared modules

❌ should not depend on individual features.

Otherwise Shared becomes another feature module.

---

# Navigation

Navigation coordinates features.

Navigation

✅ may depend on feature APIs.

Navigation

❌ should never contain business logic.

---

# Android Application

The Android application is the composition root.

Responsibilities

- start application
- initialize dependency injection
- configure Android
- host navigation

It should contain almost no business logic.

---

# Gateway

The gateway service is an independent application.

It should remain isolated from Android-specific implementation details.

Shared protocol definitions belong inside reusable modules rather than inside the gateway.

---

# Build Logic

The included build

```
build-logic/
```

is responsible for

- convention plugins
- Gradle tasks
- architecture validation
- documentation generation

Application modules should never contain duplicated Gradle configuration.

---

# Source Set Rules

Business logic belongs inside

```
commonMain
```

Platform implementations belong inside

```
androidMain
```

Only platform-specific APIs should appear in platform source sets.

This keeps the project ready for future platforms.

---

# Compose Rules

Compose code belongs inside the Presentation layer.

Compose should not appear inside

- Domain
- Repository implementations
- Room entities
- networking

Presentation renders state.

It does not own business rules.

---

# Repository Rules

Repositories expose interfaces through the Domain layer.

Implementations belong inside the Data layer.

Presentation should never access repository implementations directly.

```
ViewModel

↓

Repository Interface

↓

Repository Implementation
```

---

# ViewModel Rules

ViewModels coordinate the presentation layer.

ViewModels

✅ may depend on

- UseCases
- domain models

ViewModels

❌ should not depend on

- DAO
- Room Database
- HTTP clients
- WebSocket implementations
- repository implementations

These rules are enforced by custom Detekt rules.

---

# DAO Rules

Room DAOs are infrastructure.

They should only be used by repository implementations.

No presentation or domain code should access DAOs directly.

```
DAO

↓

Repository

↓

UseCase

↓

ViewModel
```

---

# Dependency Cycles

Circular dependencies are forbidden.

Example

```
Feature A

↓

Feature B

↓

Feature A
```

The architecture validator detects project dependency cycles during the build.

---

# Explicit Dependencies

Dependencies should always be declared explicitly.

Avoid introducing transitive dependencies as hidden implementation details.

Review every new dependency carefully.

---

# Version Management

All external library versions are managed centrally through

```
gradle/libs.versions.toml
```

Module build files should never hardcode dependency versions.

This keeps updates consistent across the project.

---

# Documentation

Whenever the dependency graph changes execute

```bash
./gradlew architectureReport
```

This regenerates

- architecture overview
- dependency matrix
- module pages
- project statistics

Generated documentation should always be committed together with architectural changes.

---

# Automated Validation

SecureChat validates

- project dependency graph
- circular dependencies
- architecture boundaries
- custom Detekt rules
- generated documentation

These checks execute automatically during the quality pipeline.

Developers do not need to remember them manually.

---

# Summary

The dependency rules exist to preserve the long-term structure of the project.

Instead of relying solely on documentation or code reviews, SecureChat enforces these rules automatically during every build.

Maintaining these boundaries keeps the project modular, testable and scalable as additional features and modules are introduced.
