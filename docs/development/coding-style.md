# Coding Style

## Overview

SecureChat follows a strict and consistent coding style.

The project values readability, maintainability and explicitness over clever or overly compact implementations.

Formatting is enforced automatically through KtLint and project-specific quality tasks.

Architecture is enforced through custom Detekt rules.

---

# General Principles

Code should be

- readable
- predictable
- explicit
- testable
- platform-independent where practical

Every source file should have a single, clear responsibility.

---

# Kotlin

Use modern Kotlin features where they improve readability.

Examples include

- data classes
- sealed interfaces
- value classes
- extension functions
- immutable collections
- coroutines
- Flow

Avoid using language features simply because they are available.

Readability always takes priority.

---

# Naming

Names should describe responsibility rather than implementation.

Good

```kotlin
SendMessageUseCase

ContactRepository

IdentityGenerator

MessageCipher
```

Avoid

```kotlin
Manager

Helper

Processor

Util

Stuff
```

If a class requires a vague suffix such as "Manager", reconsider its responsibility.

---

# Package Structure

Each feature follows

```
presentation/

domain/

data/
```

Presentation contains

- Compose
- ViewModels
- UI State

Domain contains

- UseCases
- Models
- Repository interfaces

Data contains

- Repository implementations
- Room
- Network
- Mapping

---

# File Size

Prefer small focused files.

General guideline

| Type | Recommended Size |
|-------|------------------|
| Kotlin file | < 500 lines |
| ViewModel | < 300 lines |
| Composable | < 150 lines |
| UseCase | Usually very small |

Large files usually indicate multiple responsibilities.

---

# Functions

Functions should perform one task.

Good

```kotlin
encryptMessage()

serializePacket()

loadIdentity()
```

Avoid long methods containing unrelated logic.

Extract private functions where appropriate.

---

# Classes

Every class should have a single responsibility.

Examples

```
ContactRepository

IdentityRepository

TransportClient
```

Avoid large "God Objects".

---

# Immutability

Prefer immutable objects.

Good

```kotlin
val
```

Avoid unnecessary

```kotlin
var
```

Mutable state should be localized.

---

# Nullability

Prefer explicit nullability.

Avoid

```kotlin
!!
```

SecureChat contains custom Detekt rules discouraging unsafe null handling.

Use

- null checks
- Elvis operator
- requireNotNull()
- Result types

instead.

---

# Coroutines

Prefer structured concurrency.

Avoid

- GlobalScope
- unmanaged Jobs

Launch work from appropriate lifecycle-aware scopes.

---

# Logging

Use the project-owned `SecureChatLogger` from `:core` for application and shared Kotlin code.
The JVM server services use SLF4J with Logback.

Do not use `print`, `println`, `System.out`, `System.err`, or `printStackTrace`.

Pass failures to the logger as a `Throwable` and use lazy message lambdas. Never log message
contents, phone numbers, cryptographic material, safety numbers, or encoded payloads.

See the [Logging guide](logging.md) for levels, examples, privacy rules, and extension points.

---

# Flow

Prefer Flow for asynchronous state.

Typical flow

```
Repository

↓

UseCase

↓

ViewModel

↓

StateFlow

↓

Compose
```

Do not expose mutable flows publicly.

---

# Compose

Composable functions should

- be stateless whenever possible
- receive state through parameters
- emit UI only

Business logic belongs inside ViewModels and UseCases.

Feature UI rules:

- keep one rendered component per Kotlin file;
- colocate that component's `@Preview` in the same file;
- place detailed rendering in `presentation/component/<screen-name>`;
- keep `*Screen.kt` focused on its public state and callback contract;
- keep `*Route.kt` and `*Flow.kt` focused on state collection and orchestration;
- do not place validation, repositories, crypto work, or coroutine controllers in composables.

Preview data may be shared through a non-composable `*PreviewData` object in the same component
package. Routes, flows, ViewModels, and non-UI models do not require previews.

---

# Modifier

Composable APIs should generally expose

```kotlin
modifier: Modifier = Modifier
```

as the first optional parameter.

This keeps APIs consistent across the project.

---

# Resources

Avoid hardcoded values.

Use

- string resources
- dimensions
- theme values
- icons

instead of literals.

Examples

Avoid

```kotlin
16.dp

Color.Red

"Hello"
```

Prefer centralized resources where appropriate.

---

# Dependency Injection

Use constructor injection.

Avoid service locators.

Dependencies should be explicit.

---

# Logging

Do not log

- private keys
- plaintext messages
- authentication tokens
- sensitive identity information

Logs should never expose user secrets.

---

# Error Handling

Errors should be explicit.

Prefer

- Result
- sealed classes
- domain-specific errors

Avoid swallowing exceptions.

---

# Comments

Good code should usually explain itself.

Use comments to explain

- architectural decisions
- protocol behaviour
- security rationale

Avoid comments that merely repeat the code.

---

# Testing

Every significant business rule should be testable independently.

Prefer small focused unit tests over large integration tests.

---

# Formatting

Formatting is handled automatically.

Execute

```bash
./gradlew qualityFix
```

before committing if necessary.

Formatting changes should never be performed manually.

---

# Architecture

Do not violate architectural boundaries.

Presentation should never access

- Room
- DAO
- repository implementations

Business logic should remain inside the Domain layer.

The build validates these rules automatically.

---

# Summary

SecureChat values consistency over individual coding preferences.

Following these conventions makes the codebase easier to understand, simplifies reviews and allows automated tooling to enforce project-wide standards.
