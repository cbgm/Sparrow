# Module Types

## Overview

SecureChat is intentionally divided into several module types.

Each module type has a well-defined responsibility and dependency direction.

Choosing the correct module type is one of the most important architectural decisions when adding new functionality.

---

# Module Hierarchy

```
Android Application

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

Lower layers should never depend on higher layers.

---

# Android Application

## Purpose

The Android application is the composition root.

It is responsible for creating the application and wiring every other module together.

Typical responsibilities include

- `Application`
- `MainActivity`
- Android Manifest
- Android resources
- Dependency Injection bootstrap

---

## Allowed Dependencies

The Android application may depend on

- Shared
- Navigation
- Feature
- Startup
- Core

It should contain almost no business logic.

---

# Startup

## Purpose

The startup module initializes the application.

Typical responsibilities

- application startup
- initialization
- startup checks
- application bootstrap

Startup should prepare the application for execution.

It should not implement business features.

---

# Shared

## Purpose

Shared modules contain functionality used by multiple features.

Typical examples

- reusable Compose UI
- common ViewModels
- reusable application services
- common utilities

---

## When to Create a Shared Module

Create a Shared module when

- multiple features require the same implementation
- functionality is application-specific
- the code is not reusable enough for Core

---

## Avoid

Shared modules should not evolve into another feature module.

Avoid adding

- feature-specific business rules
- feature-specific navigation
- feature-specific repositories

---

# Navigation

## Purpose

Navigation coordinates application flow.

Typical responsibilities

- routes
- navigation graph
- destination registration
- navigation helpers

Navigation should not contain business logic.

---

# Feature

## Purpose

Feature modules implement user-visible functionality.

Examples

```
Chats

Contacts

Identity

Transport

Onboarding
```

Each feature owns

- Presentation
- Domain
- Feature-specific Data

---

## Typical Structure

```
feature/

presentation/

domain/

data/
```

Every feature should follow the same structure.

Consistency reduces maintenance cost.

---

## When to Create a Feature

Create a new feature when

- new user-visible functionality is added
- the feature has independent business logic
- the feature can evolve independently

Avoid creating tiny feature modules for unrelated helper classes.

---

# Data

## Purpose

Data modules provide implementations.

Examples

- Room
- repositories
- local persistence
- remote APIs
- transport

Business decisions should not originate here.

---

## Responsibilities

Data modules

- load data
- save data
- map models
- communicate with external systems

They do **not**

- own business rules
- contain Compose UI
- expose Android-specific APIs unnecessarily

---

# Core

## Purpose

Core modules contain reusable libraries.

Typical examples

```
core:crypto

core:id

core:protocol

core:ui

core:recommendations
```

These modules should remain reusable across multiple features.

---

## Characteristics

Core modules

- stable
- reusable
- platform independent whenever possible
- small
- focused

A Core module should have a single responsibility.

---

# Gateway

## Purpose

The gateway is an independent server application.

Responsibilities

- WebSocket server
- client registration
- message forwarding
- transport infrastructure

The gateway should not contain Android-specific code.

Shared protocol definitions belong inside reusable Core modules.

---

# Quality

## Purpose

The Quality module contains SecureChat-specific static analysis.

Examples

- custom Detekt rules
- rule providers
- rule tests

General application logic does not belong here.

---

# Build Logic

## Purpose

The included build

```
build-logic/
```

configures the entire project.

Responsibilities

- convention plugins
- Gradle tasks
- architecture validation
- documentation generation
- quality automation

Developers should prefer extending convention plugins instead of duplicating Gradle configuration across modules.

---

# Documentation

## Purpose

```
docs/
```

contains the engineering handbook.

Documentation is divided into

- Getting Started
- Architecture
- Build
- Development
- Security
- Features
- API
- Generated Reference

Generated documentation is produced automatically by

```bash
./gradlew architectureReport
```

and should never be edited manually.

---

# Choosing the Correct Module

The following table can be used as a guideline.

| If the code... | Module Type |
|----------------|------------|
| Is reusable across the project | Core |
| Implements persistence or networking | Data |
| Represents user-visible functionality | Feature |
| Is shared between several features | Shared |
| Coordinates navigation | Navigation |
| Starts the application | Startup |
| Boots Android | Android App |
| Extends the build | Build Logic |
| Implements gateway infrastructure | Gateway |
| Adds static analysis | Quality |

---

# Decision Guidelines

When introducing new functionality, ask the following questions.

1. Is this reusable outside a single feature?
2. Does it implement business logic?
3. Does it access persistence?
4. Does it belong to Android?
5. Does it configure the build?
6. Does it provide developer tooling?

The answers generally identify the correct module type.

---

# Architectural Evolution

As SecureChat grows, new modules should fit into the existing architecture rather than introducing new architectural concepts.

Adding another Feature module is usually preferable to expanding an existing feature with unrelated responsibilities.

Likewise, reusable functionality should be extracted into Core or Shared rather than duplicated.

Keeping the module hierarchy consistent ensures that the architecture remains understandable and scalable over time.

---

# Summary

Each SecureChat module type has a clearly defined purpose.

Selecting the correct module type keeps responsibilities explicit, dependency direction predictable and the overall architecture maintainable as the project continues to grow.
