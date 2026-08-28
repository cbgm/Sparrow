# Coding style

The objective is readable Kotlin with obvious ownership. Architecture rules matter more than arbitrary metrics.

## Kotlin

Use project formatting/static-analysis tasks and keep code idiomatic. Prefer clear names over comments that repeat the code.

```bash
./gradlew qualityCheck
```

## Layer/model naming

Use layer-specific target names consistently:

| Layer | Model | Mapper to that layer |
|---|---|---|
| data | `NameDto` | `toNameDto()` |
| domain | `Name` | `toName()` |
| presentation | `NameUi` | `toNameUi()` |

Examples:

```kotlin
fun ContactRow.toContactDto(): ContactDto
fun ContactDto.toContact(): Contact
fun Contact.toContactUi(): ContactUi
```

Room persistence types remain `NameEntity`; explicit packet/protocol types keep protocol names. Do not rename constants/configuration objects to DTOs just because they are in a data package.

Do not use generic mapper names such as `toDomain`, `toUi`, `toUiModel` or `toUiState` when the concrete target type has a name.

## Architecture naming

- repository contract: `SomethingRepository`;
- repository implementation: `SomethingRepositoryImpl`;
- one-purpose domain action: `SomethingUseCase`;
- data representation: `SomethingDto`;
- presentation representation: `SomethingUi`;
- state transition logic: `SomethingStateMachine` where a lifecycle really is a state machine;
- mapper packages contain mappings;
- handler names describe exactly what they handle.

Do not create generic interfaces/handlers simply to make the code look architected.

## Dependency direction

- ViewModels call use cases.
- Use cases call repository contracts, never other use cases.
- Repository implementations do not call other repositories or use cases.
- Datasources do not call repositories.
- Platform code stays in platform source sets.

## Direct vs Group

Never merge Direct and Group messaging/membership/delivery/typing implementations just because they expose similarly named operations. See [Chats architecture](../architecture/chats.md).

## Functions/files

There is no arbitrary limit such as “a file may only have N functions.” Refactor based on responsibility, function size, readability and ownership. Do not let one file accumulate unrelated orchestration, mapping, persistence, protocol and UI responsibilities.

## Packages

Keep small `model`, `mapper`, `repository`, `datasource` and domain package families flat. Add nested grouping only when the file set is genuinely large enough to benefit from it.

Platform-specific implementation belongs under the owning top-level `device` responsibility in `androidMain`/`iosMain`, not in `commonMain` or a nested `data/device` package.

## Compose

- Keep route/state collection separate from detailed rendering where useful.
- Put group-only components in the group-specific package; put truly reusable chat components in the shared component package.
- Keep each required `@Preview` in the **same file as its composable**.
- Do not move business rules into composables.

## ViewModels

ViewModels invoke use cases and expose presentation state/effects. They should not call DAOs, datasources, repository implementations or network clients directly.
