# Development workflow

## Branch model

The current repository flow is:

```text
feature/*
   │ PR
   ▼
develop
   │ stabilization/integration
   ▼
master
   │ create release line
   ▼
release/0.1, release/0.2, ...
   │ tag exact release commit
   ▼
v0.1.0-alpha.1, v0.1.0, ...
```

Feature work normally targets `develop`. The Android PR workflow runs for PRs into `develop` and builds a debug
APK plus quality/tests/device tests.

`master` is not used as a branch-prefix for release branches. Git cannot have both `master` and
`master/release/...` refs, so release branches use `release/x.y` and are created from `master`.

## Day-to-day loop

1. Branch from the appropriate integration point.
2. Implement one cohesive change.
3. Run focused tests while working.
4. Before opening/updating the PR, run:

```bash
./gradlew qualityCheck
./gradlew allTests
```

5. If module dependencies changed:

```bash
./gradlew architectureReport
./gradlew verifyArchitectureReport
```

6. Open a PR into `develop`.

## Chat changes

Before changing chat behavior, read [Chats architecture](../architecture/chats.md). Direct and Group behavior must remain
separate. A similarly named operation is not enough reason to introduce a generic abstraction.

## Server changes

Server services are independent Gradle modules and Docker images. Test the affected service and then run the
appropriate smoke test. See [Testing](../development/testing.md).

## Release lines

Every push to `release/**` runs the release workflow. It detects changed paths and builds only affected
artifacts. A `v*` tag forces a complete reproducible release build. See
[Release process](../development/release-process.md).
