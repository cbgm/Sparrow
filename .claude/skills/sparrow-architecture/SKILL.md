---
name: sparrow-architecture
description: Rules for every code change in the Sparrow app (Kotlin Multiplatform modules under core, data, feature, protocol, shared, startup, navigation). Use before writing or reviewing any Sparrow client code.
---

# Sparrow architecture

The rules below are the short form. The long form lives in `docs/architecture/` (start with `dependency-rules.md`,
`clean-architecture.md`, `module-types.md`) and `docs/development/coding-style.md`; read the page for the area you
touch. Part of this is enforced by the custom detekt rules in `quality/detekt-rules`.

## Modules

- Every behaviour has exactly one owning module. Look it up in `docs/architecture/module-types.md` before adding code.
- `core/*` only holds what really every module uses. Feature-specific components and helpers stay in their feature.
  Do not add files to `core` unless it is needed.
- `androidApp` stays thin; platform code goes into the owning module's `androidMain` / `iosMain` under `device/`.
- No dependency cycles; a lower module never reaches up for convenience. Feature-to-feature dependencies only where
  `docs/architecture/dependency-rules.md` allows them.
- Conversation writes go through `:feature:conversationorchestration:*` (commands and workflows); `:feature:chats`
  only presents and reads.
- Only `:feature:chats` may call the orchestration, through `ConversationOrchestrator` in
  `:feature:conversationorchestration:api`. No other feature depends on any orchestration module; `shared` only
  registers their Koin modules and starts their runtime.
- One way of integrating a feature into another (like polls through a composer repository), not a different trick
  each time.

## Packages inside a module

```text
domain/model         unsuffixed domain models
domain/repository    interfaces for data access (storage, network, platform data)
domain/port          interfaces for running processes (connections, background work)
domain/usecase       one job per use case
domain/rule          pure logic without IO
data/repository      …RepositoryImpl / port implementations
data/datasource      storage, network and platform access
data/dto, data/entity, data/mapper
presentation         ViewModel, …UiState, …UiEvent, component/, mapper/, model/
device               platform implementations (androidMain / iosMain)
di                   Koin modules
```

- Only `domain` is public. Everything in `data`, `device` and `presentation` is `internal` unless another module
  truly needs it.
- Transport (`:feature:transport`) still uses `domain/gateway`; it is being migrated to `repository` / `port`.

## Layers

- **Domain** imports no Compose, Room, Ktor, serialization annotations or platform APIs. Wire formats are `…Dto` in
  `data`, mapped to domain models.
- **Use cases** do one thing (`operator fun invoke`), never call other use cases, and return `Result` through
  `safeSuspendCall` / `safeCall`; only observing use cases return a `Flow`. Use cases hold no state of their own.
- **Repositories** return `Result` (or `Flow`), own their datasources and mappers, and never call other repositories
  or use cases.
- **Datasources** only talk to storage, network or the platform; they never call repositories.
- **ViewModels** only use use cases: no repository, datasource, DAO or port directly.
- Cross-feature sequences belong in a workflow, not in a repository or datasource.

## Naming

- Suffixes by layer: `…Entity` for database (Room) models, `…Dto` for every other model in the data layer (network,
  wire, DataStore, files), `…Ui` for presentation models; domain models carry just the name (`Contact`, `Message`).
- Mappers name their target: `toX()`, `toXDto()`, `toXUi()`.
- Interfaces: `…Repository` for data, `…Port` for running processes. Implementations: `…RepositoryImpl`;
  datasources say how they reach the data (`…RemoteDataSource`, `…LocalDataSource`, `DataStore…`).
- Forbidden class names and suffixes: `Coordinator`, `Provider`, `Settings`, `Manager`, `Default…`, `…Impl` outside
  repositories.
- No wrappers (a route or class that only wraps another one) and no no-op handlers.
- No duplicate structures: reuse existing models (`Image` / `ImageDto` and the like) instead of creating look-alikes.

## Persistence

- Per-contact and per-group data goes into Room tables, not DataStore. DataStore is for single app-wide values.
- The app is not live yet: no database migrations.
- Secrets that only matter for a moment (for example a verification token) stay in memory and are never stored.

## Code style

- No `if` directly inside a `when` branch: the branch calls its own function, which does the check.
- No `!!`.
- No comments that repeat the code. KDoc explains why something is the way it is.
- Imports sorted alphabetically, no unused imports (ktlint does not remove them: check by hand, but keep
  `getValue` / `setValue` delegate imports).
- A Compose `Modifier` parameter is called `modifier`, has a default and is the first optional parameter.
- Previews live in the same file as their composable.
- Keep changes minimal and in the style of the code around them.
- Never touch `local.properties`.

## UI

- One design for the whole app: fields, buttons, top bars, dialogs and lists come from `core:ui` and look the same
  everywhere (onboarding may differ). Design changes go through the existing core components.
- Before writing a component, helper or model, search `core:ui` (`component/`, `theme/`, `helper/`, `animation/`),
  `core:util` and `core:base` for one that already does it, and use or extend that. Only if nothing fits: a component
  used by one feature stays in that feature; one that several features need goes into core.
- Every screen has 16dp padding before the content starts.
- Show a mockup before any design change. A redesign changes only the design, never the behaviour.
- Strings live in `resources/src/commonMain/composeResources/values*/strings_<feature>.xml`, named after the key
  prefix (`feature_chats_…` → `strings_chats.xml`, shared ones in `strings.xml`). Always English and German
  together.

## Privacy and security

- Never expose the user's IP address; requests that leave the device go through the user's route protection.
- Servers must not learn more about users than the request needs.
- Never claim in the UI that messages are always encrypted.

## Before handing over

1. Compile every module that depends on what changed.
2. Run `ktlintFormat` on the changed modules and check for unused imports by hand.
3. Run the tests of the changed modules (`./gradlew allTests`); `./gradlew qualityCheck` runs detekt and the rest.
4. After module dependency changes: `./gradlew architectureReport verifyArchitectureReport`.
5. Never commit for the user and never push: he commits himself. Suggest a commit name.
6. Explain the change with every detail up front: what changed, why, what was left and why, what he must check.
