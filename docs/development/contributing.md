# Contributing

## Branch flow

```text
feature/*
   -> pull request into develop
   -> develop integration/testing
   -> master when stable
   -> release/x.y created from master
   -> vX.Y.Z[-prerelease] tag for a full GitHub release
```

Do not build distributable releases from arbitrary feature branches.

## Before opening a PR

At minimum run the checks relevant to your change:

```bash
./gradlew qualityCheck
./gradlew allTests
```

For Android/platform changes, run Android device tests where applicable. For server changes, run the relevant Docker smoke test.

## Architecture changes

If module dependencies change:

```bash
./gradlew architectureReport
./gradlew verifyArchitectureReport
```

Commit regenerated `docs/generated/` output; do not hand-edit it.

## Architecture and naming changes

Follow the project layer conventions:

- data representation: `NameDto` / `toNameDto()`;
- domain representation: `Name` / `toName()`;
- presentation representation: `NameUi` / `toNameUi()`;
- ViewModel -> use case -> repository contract;
- no use-case-to-use-case, repository-to-repository/use-case, or datasource-to-repository calls.

See [Clean architecture](../architecture/clean-architecture.md) and [Coding style](coding-style.md).

## Chat changes

Read [Chats architecture](../architecture/chats.md) first. Keep Direct and Group responsibilities separate. Keep attachment source/blob ownership in `:feature:attachments` and map chat content through `MessagePartDto` -> `MessagePart` -> `MessagePartUi`.

## Documentation changes

Update the closest handwritten document when behavior/configuration changes. Keep links relative so MkDocs and GitHub rendering both work.

## Documentation location

Keep project documentation centralized:

- `README.md` at the repository root is the only project Markdown documentation outside `docs/`;
- `server/secrets/placeholder.md` is a non-documentation placeholder whose only purpose is to keep the empty secrets directory tracked by Git;
- all architecture, feature, server, operations, security, API and development documentation belongs under `docs/`;
- do not add module-local `README.md` or `ARCHITECTURE.md` files;
- update `mkdocs.yml` when adding a user-facing documentation page;
- keep generated architecture reference under `docs/generated/` and regenerate it with the architecture-report tasks.

This prevents duplicated feature/server documentation from drifting away from the central source of truth.
