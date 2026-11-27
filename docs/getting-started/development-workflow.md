# Development workflow

## Branch model

The release branch flow is:

```text
feature/* -> PR -> develop -> merge into release/0.6
                                  │
                                  ├─ beta / RC tags
                                  └─ v0.6.0 final tag
                                         │
                                         └─ merge into master and develop
```

Keep `master` stable for published releases, and use `develop` for ongoing feature work and integration. Feature work normally targets `develop`. The Android PR workflow runs for PRs into `develop` and builds a debug APK plus quality/tests/device tests.

For 0.6.0, use one `release/0.6` branch throughout beta, RC and final preparation. Start it from
the updated `master`, merge `develop` into it, and merge release-specific fixes back into
`develop`. Stable releases and post-release hotfixes update `master`; see the complete
[Release process](../development/release-process.md).

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

## Release builds

The full release workflow runs on pushed `v*` tags and builds the exact tagged commit. Beta and RC
tags must be reachable from `release/*`; stable tags may be reachable from `release/*` or
`master`. See [Release process](../development/release-process.md).
