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

## Chat changes

Read [Chats architecture](../architecture/chats.md) first. Keep Direct and Group responsibilities separate, and use existing state machines/coordinators instead of adding cross-cutting handlers that blur the red line.

## Documentation changes

Update the closest handwritten document when behavior/configuration changes. Keep links relative so MkDocs and GitHub rendering both work.

## Documentation location

Keep project documentation centralized:

- `README.md` at the repository root is the only project Markdown documentation outside `docs/`;
- all architecture, feature, server, operations, security, API and development documentation belongs under `docs/`;
- do not add module-local `README.md` or `ARCHITECTURE.md` files;
- update `mkdocs.yml` when adding a user-facing documentation page;
- keep generated architecture reference under `docs/generated/` and regenerate it with the architecture-report tasks.

This prevents duplicated feature/server documentation from drifting away from the central source of truth.
