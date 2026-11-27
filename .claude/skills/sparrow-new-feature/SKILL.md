---
name: sparrow-new-feature
description: Step-by-step for adding a new feature, screen or module to the Sparrow app. Use when the user asks for something Sparrow cannot do yet; use sparrow-architecture for the rules that apply on the way.
---

# New feature in Sparrow

Follow `sparrow-architecture` for every rule. This skill is the order of work. The checklist in
`docs/development/extending.md` covers special cases (attachments, Direct and Group chat behaviour, packets).

## 1. Clarify before writing code

- What exactly should the user be able to do, on which screens, and what happens on failure or offline?
- Does it change what leaves the device or what a server learns? Then the privacy question is settled first.
- Which module owns it? Read `docs/architecture/module-types.md`. Prefer an existing module; a new module only for a
  real independent responsibility, never for one screen.
- Does it touch conversations? Writes become commands and workflows in `:feature:conversationorchestration:*`
  (see `docs/architecture/conversation-orchestration.md`), and only `:feature:chats` submits them. A new feature
  never calls the orchestration itself: its screen lives in chats, or chats triggers it.
- Does it need a new packet? Follow "Add a new application packet" in `docs/development/extending.md`.

## 2. Show a mockup

Any new or changed screen gets a mockup (HTML rendered to PNG) before code. Use the app's colours, the 16dp screen
padding and only components that exist in `core:ui`. Wait for the user's choice.

## 3. Module (only if a new one is needed)

- `feature/<name>/build.gradle.kts` with the convention plugin: `sparrow.kmp.compose.feature` for a module with
  screens, `sparrow.kmp.library` without; add `sparrow.kmp.testing`, `sparrow.kmp.serialization` or
  `sparrow.kmp.room` as needed. Namespace `com.cbgm.sparrow.feature.<name>`.
- `include(":feature:<name>")` in `settings.gradle.kts`.
- Only the dependencies the rules allow; check with `./gradlew architectureReport verifyArchitectureReport`.

## 4. Domain

- Models in `domain/model`, reusing existing ones where they exist.
- `domain/repository` interfaces for data, `domain/port` for running processes, only if needed.
- One use case per action in `domain/usecase`, returning `Result` (or `Flow` when observing).
- Pure decisions in `domain/rule`, with unit tests.

## 5. Data

- `…Dto` / `…Entity` and their mappers in `data/dto`, `data/entity`, `data/mapper`.
- `data/datasource` for storage, network and platform access; `data/repository/…RepositoryImpl` on top.
- Per-contact or per-group data in Room tables; no migrations (the app is not live).
- Network requests go through the existing transport (circuits / relays), never a direct client of their own.
- Everything here is `internal`.

## 6. DI

- `di/<Name>Module.kt` with Koin; register the module in `shared/.../di/ApplicationModules.kt`.

## 7. Presentation

- `presentation/<screen>/` with `<Screen>ViewModel` (use cases only), `model/<Screen>UiState`,
  `model/<Screen>UiEvent`, `<Screen>Route` and `<Screen>Screen`, mappers `toXUi()`.
- Only `core:ui` components; previews in the same file; 16dp screen padding.
- Before writing a new component or helper, search `core:ui`, `core:util` and `core:base` for an existing one and
  use or extend it. A new one stays in the feature unless several features need it; then it goes into core.
- Navigation: a new `AppRoute` in `core/ui/.../navigation/AppRoute.kt` and its entry in `:navigation`.
- Strings: a new `strings_<feature>.xml` in `values` and `values-de`, keys prefixed `feature_<feature>_`.

## 8. Tests

- Unit tests for rules and use cases, repository tests with fakes of the datasources, ViewModel tests for the main
  events. Fakes in tests only.

## 9. Docs

- A page in `docs/features/<feature>.md`, an entry in `docs/features/current-features.md`, and an update of
  `docs/architecture/` if a rule or boundary changed.

## 10. Hand over

Run the checklist "Before handing over" from `sparrow-architecture`, then suggest a commit name.
