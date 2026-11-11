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
.\server\scripts\Test-SparrowNetwork.ps1 -Start -BuildImages
```

Two standalone Community Nodes plus Control Plane:

```powershell
.\server\scripts\Test-StandaloneCommunityNodes.ps1 -BuildImages
```

The standalone smoke test starts a Control Plane and two Community Nodes, checks their health, that both register with their own identity, that they share no Docker network or volume, and that a node keeps its identity across a restart.

## CI branch behavior

Normal feature work opens PRs into `develop`. `.github/workflows/android-pr.yml` validates those PRs and uploads its debug APK artifact. Existing server workflows also target the `develop` integration path.

Release packaging is separate and runs on `release/**`; see [Release process](release-process.md).

## Test production behavior, not stale assumptions

A failing test is not by itself proof that production behavior is wrong. When architecture intentionally changes, update stale tests rather than modifying production solely to preserve obsolete expectations.
