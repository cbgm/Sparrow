# Introduction

## Welcome

Welcome to SecureChat.

SecureChat is a modern end-to-end encrypted messaging application built with Kotlin Multiplatform.

The project emphasizes

- privacy
- modular architecture
- maintainability
- automation
- developer experience

Unlike many applications where the build system, architecture and documentation evolve independently, SecureChat treats them as equally important parts of the project.

---

# Vision

The long-term vision of SecureChat is to provide a communication platform that is

- secure by default
- easy to maintain
- fully documented
- cross-platform
- independently auditable

Every architectural decision supports one or more of these goals.

---

# Project Philosophy

SecureChat follows several guiding principles.

## Privacy First

User privacy is the primary design objective.

Private keys remain on the device.

Messages are encrypted before transmission.

Gateway and node infrastructure are treated as untrusted.

---

## Simplicity

Simple systems are easier to

- understand
- review
- test
- secure

Whenever multiple solutions exist, the simpler solution is generally preferred.

---

## Modularity

The project is intentionally divided into many Gradle modules.

Each module has

- a clear responsibility
- explicit dependencies
- minimal coupling

This keeps the project maintainable as it grows.

---

## Automation

Automation replaces repetitive manual work wherever possible.

Examples include

- formatting
- architecture validation
- documentation generation
- quality verification
- Git hooks

Developers should focus on writing application code rather than maintaining tooling.

---

# Technology Stack

SecureChat is primarily built using

- Kotlin
- Kotlin Multiplatform
- Compose Multiplatform
- Koin
- Ktor
- Room
- Gradle Convention Plugins

These technologies provide a modern, strongly typed and multiplatform development experience.

---

# Architecture

The project follows Clean Architecture.

```
Presentation

↓

Domain

↓

Data
```

Business rules remain independent from Android and infrastructure.

Architecture is enforced automatically during the build.

---

# Security

Security is not implemented as an isolated feature.

Instead it is integrated throughout the architecture.

Core principles include

- end-to-end encryption
- cryptographic identities
- Safety Numbers
- secure key storage
- authenticated transport

Every message is encrypted before leaving the device.

---

# Build System

The SecureChat build infrastructure is highly automated.

Major features include

- Convention Plugins
- Architecture Plugin
- Quality Plugin
- Version Catalog
- Generated Documentation
- Repository-managed Git hooks

Most modules require very little Gradle configuration.

---

# Documentation

Documentation is divided into

- handwritten engineering guides
- automatically generated architecture documentation

Generated documentation is produced directly from the Gradle project, ensuring that architecture documentation always matches the repository.

---

# Repository Layout

The project is organized into several top-level areas.

```
androidApp/

build-logic/

core/

data/

feature/

navigation/

shared/

server/

quality/

docs/
```

Each directory has a clearly defined purpose.

---

# Development Workflow

Typical workflow

```
Create Feature

↓

Implement

↓

Run quality

↓

Run tests

↓

Generate documentation

↓

Commit
```

Commands

```bash
./gradlew quality
```

```bash
./gradlew architectureReport
```

---

# Intended Audience

This documentation is intended for

- new contributors
- maintainers
- reviewers
- future project members

It should provide enough context to understand both the implementation and the reasoning behind it.

---

# Learning Path

Recommended reading order

1. Introduction
2. Project Structure
3. Local Development
4. Architecture
5. Security
6. Features
7. Development
8. Build
9. API

Generated documentation should be consulted whenever detailed module information is required.

---

# Contributing

Before contributing

- read the architecture overview
- understand Clean Architecture
- follow the coding standards
- execute the quality pipeline
- regenerate architecture documentation when necessary

Maintaining consistency is considered part of every contribution.

---

# Summary

SecureChat combines modern Kotlin Multiplatform development with a strong emphasis on architecture, security and automation.

The project is designed to remain understandable and maintainable even as it grows, allowing developers to focus on building secure communication features rather than managing infrastructure complexity.
