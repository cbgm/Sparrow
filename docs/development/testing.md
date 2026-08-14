# Testing

## Local checks

Common build/test entry points:

```bash
./gradlew build
./gradlew qualityCheck
./gradlew allTests
```

Android device tests:

```bash
./gradlew connectedCheck
```

Architecture report validation:

```bash
./gradlew architectureReport
./gradlew verifyArchitectureReport
```

## Server smoke tests

PowerShell scripts under `server/scripts/` exercise the actual Compose/server topology.

```powershell
.\server\scripts\Test-SecureChatNetwork.ps1 -Start -BuildImages
```

Two standalone Community Nodes plus Control Plane:

```powershell
.\server\scripts\Test-StandaloneCommunityNodes.ps1 -BuildImages
```

The standalone smoke test accepts an initial `QUEUED_AT_GATEWAY` result and waits for asynchronous federation/retry storage at the destination rather than incorrectly requiring synchronous `STORED_AT_DESTINATION` on the first attempt.

## CI branch behavior

Normal feature work opens PRs into `develop`. `.github/workflows/android-pr.yml` validates those PRs and uploads its debug APK artifact. Existing server workflows also target the `develop` integration path.

Release packaging is separate and runs on `release/**`; see [Release process](release-process.md).

## Test production behavior, not stale assumptions

A failing test is not by itself proof that production behavior is wrong. When architecture intentionally changes, update stale tests rather than modifying production solely to preserve obsolete expectations.
