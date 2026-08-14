# Coding style

The objective is readable Kotlin with obvious ownership. Architecture rules matter more than arbitrary metrics.

## Kotlin

Use project formatting/static-analysis tasks and keep code idiomatic. Prefer clear names over comments that repeat the code.

```bash
./gradlew qualityCheck
```

## Architecture naming

- repository contract: `SomethingRepository`;
- repository implementation: `SomethingRepositoryImpl`;
- one-purpose domain action: `SomethingUseCase`;
- state transition logic: explicit `SomethingStateMachine` where a lifecycle really is a state machine;
- mapper packages contain mappings;
- handler names should describe exactly what they handle.

Do not create generic interfaces/handlers simply to make the code look architected.

## Direct vs Group

Never merge Direct and Group messaging/membership/delivery/typing implementations just because they expose similarly named operations. See [Chats architecture](../architecture/chats.md).

## Functions/files

There is no arbitrary limit such as “a file may only have N functions.” A file with 15 cohesive functions can be better than five tiny files. Refactor based on responsibility, function size, readability and ownership.

## Compose

- Keep route/state collection separate from detailed rendering where useful.
- Put group-only components in the group-specific package; put truly reusable chat components in the shared component package.
- Keep each required `@Preview` in the **same file as its composable**.
- Do not move business rules into composables.

## ViewModels

ViewModels invoke use cases and expose presentation state/effects. They should not call DAOs/repository implementations/network clients directly.
