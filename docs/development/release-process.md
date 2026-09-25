# Release process

Sparrow has release automation configured, but **no official tagged full release has been published yet**.
This page describes the workflow that will create the first and later releases.

## Branches

Normal development:

```text
feature/* -> PR -> develop -> master
```

Create a release line from `master`:

```text
release/0.1
release/0.2
release/1.0
```

Do not use `master/release/...`; Git refs cannot coexist with a `master` branch in that shape.

## Repository variables

Configure in GitHub Actions **Variables**:

```text
CONTROL_PLANE_DIRECTORY_URL
CONTROL_PLANE_RELEASE_DIRECTORY_URL
```

The debug/PR workflow reads repository variable `CONTROL_PLANE_DIRECTORY_URL` and writes the local development property `controlPlaneDirectoryUrl=...`. The signed release workflow reads `CONTROL_PLANE_RELEASE_DIRECTORY_URL` and writes `CONTROL_PLANE_RELEASE_DIRECTORY_URL=...` for the BuildKonfig release flavor. Both flavors expose the selected value to common code as `BuildKonfig.CONTROL_PLANE_DIRECTORY_URL`.

## Android signing secrets

Configure GitHub Actions **Secrets**:

```text
KEY_STORE_FILE
KEY_STORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

`KEY_STORE_FILE` is the Base64-encoded `.jks` file, not a filesystem path or password. Keep the original keystore in
a secure backup; losing the signing key prevents future APKs from being installed as updates to the same app.

Example key creation in Windows CMD:

```cmd
keytool -genkeypair -keystore sparrow-release.jks -alias sparrow -keyalg RSA -keysize 4096 -validity 10000
```

The key password and keystore password may be the same if desired.

Convert the keystore to Base64 for GitHub. Windows PowerShell:

```powershell
[Convert]::ToBase64String(
    [IO.File]::ReadAllBytes("sparrow-release.jks")
) | Set-Content "sparrow-release-keystore-base64.txt"
```

macOS/Linux:

```bash
base64 < sparrow-release.jks > sparrow-release-keystore-base64.txt
```

Paste that file's contents into `KEY_STORE_FILE`. `KEY_STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` contain the actual keystore password, alias, and key password. Back up the original `.jks` offline; do not commit it.

## Complete release command sequence

Use this sequence when the current `develop` state is ready to become a release candidate. The normal Sparrow workflow keeps release work flowing in one direction: `develop -> master -> release/x.y -> tag`.

### 1. Update local `develop` from origin

Start from the integration branch and pull the current remote state:

```powershell
git checkout develop
git status
git pull origin develop
```

`git status` should be clean before continuing. Do not start the release procedure with an uncommitted working tree.

### 2. Merge `develop` into `master` with the exact `develop` tree

Update local `master`, create a real merge relationship with `develop`, then replace the merge result with the exact tree from `develop` before committing:

```powershell
git checkout master
git pull origin master

git merge develop -s ours --no-commit --no-ff
git read-tree --reset -u develop
git commit -m "Merge develop into master"

git push origin master
```

This intentionally avoids manual conflict resolution. `git merge ... -s ours --no-commit --no-ff` records both branch histories as a merge without trying to combine their file contents. `git read-tree --reset -u develop` then makes the pending merge tree exactly match `develop`, and the explicit commit creates the release merge commit `Merge develop into master`. The result is therefore a real merge commit whose project contents are identical to the tested `develop` state.

### 3. Create a new release branch from the updated `master`

For a new `0.1` release line:

```powershell
git checkout master
git pull origin master
git checkout -b release/0.1
git push -u origin release/0.1
```

Every later push to that branch creates or updates release-candidate artifacts according to the changed files below. It does **not** publish the official GitHub Release yet.

If the release branch already exists, do not create it again. Update it from `master` instead:

```powershell
git checkout release/0.1
git pull origin release/0.1
git merge master -X theirs
git push origin release/0.1
```

Here `release/0.1` is `ours` and `master` is `theirs`, so conflicting hunks prefer the newly promoted `master` state.

### 4. Stabilize and test the release branch

Any release-specific fix is committed and pushed on the release branch:

```powershell
git checkout release/0.1
git pull origin release/0.1

# make/fix/test the release changes

git add .
git commit -m "Fix release issue"
git push origin release/0.1
```

Each push to `release/**` creates a new release candidate. Test the candidate artifacts before tagging.

### 5. Tag the exact tested release commit

For an alpha release:

```powershell
git checkout release/0.1
git pull origin release/0.1
git status
git tag v0.1.0-alpha.1
git push origin v0.1.0-alpha.1
```

For a stable release, use the stable semantic-version tag instead:

```powershell
git checkout release/0.1
git pull origin release/0.1
git status
git tag v0.1.0
git push origin v0.1.0
```

The tag must point at the exact release-branch commit whose candidate was tested. A `v*` tag triggers the complete release build described below.

## Release automation present in this source snapshot

The source archive used for this documentation audit does **not** contain the repository `.github/workflows` directory. Therefore this documentation does not pretend to verify the exact current GitHub Actions trigger/change-classification implementation from this ZIP alone.

What **is** directly verifiable in the source is the current Android build configuration, server Dockerfiles/Compose definitions and the unified public server-bundle builder under `server/unified`.

If the CI workflow in GitHub is changed, keep this page synchronized with the checked-in workflow files in the repository used for release.

## Current public server bundle

The current server distribution path is a **single unified server bundle**, not separate public Control Plane and Community Node packages.

Build from the repository root on Windows:

```text
server\unified\Build-SparrowServer.cmd
```

That wrapper invokes `server/unified/New-SparrowServerBundle.ps1` and produces:

```text
dist/sparrow-server.zip
```

The bundle contains the cross-platform runtime/manager scripts plus the Control Plane and Community Node Compose/Caddy configuration needed by the unified manager. It does not contain live deployment secrets or state.

Important bundled entry points include:

- `Start-SparrowServer.ps1`
- `Start-SparrowServer.sh`
- `Start-SparrowServer.command`
- `Invoke-SparrowServer.ps1` / `.py`
- `Manage-SparrowNodes.ps1` / `.py`
- the Control Plane and Community Node Compose/Caddy templates
- signed-directory client/registration helpers used by the public runtime
- shared-public-proxy definitions used by public Combined deployments

The Windows bundle also exposes `Start-SparrowServer.cmd` as the normal GUI entry point after packaging.

## Server bundle exclusion: private Control Plane Directory

`server/control-plane-directory` is **operator-only** infrastructure. `New-SparrowServerBundle.ps1` explicitly documents that it must never copy that directory or server identities/secrets into the public unified bundle.

Therefore these are **not public Sparrow server release assets**:

- the independent directory's private installer;
- its administrator CLI/token;
- its signing key or SQLite state;
- the persistent `sparrow-central-directory-state` volume.

The public unified runtime may contain directory **client** helpers, because nodes/Control Planes need to consume/register with a separately operated signed directory. That is different from shipping the directory server itself.

## Server image builds

Executable Ktor services are built with the shared `server/Dockerfile` using a `SERVICE` build argument. Current Gradle server modules are:

```text
:server:node-registry
:server:presence-directory
:server:gateway
:server:federation
:server:mailbox
:server:push
:server:link-preview
```

They share `:server:protocol`, `:server:security`, `:server:persistence` and `:server:observability` as appropriate.

The unified manager is designed to pull/update selected Sparrow backend images while preserving an intact installation's stateful PostgreSQL/Redis/Caddy resources, generated secrets and identities. That lifecycle behavior does not remove the need for a verified backup before an upgrade that may run database migrations.

## Android release APK

The Android application build uses the `androidApp` release configuration. Release signing requires the keystore material described earlier on this page. Release builds use the configured R8/resource-shrinking rules from the Android Gradle build.

Before publishing, preserve the release signing keystore independently. If obfuscation is enabled for the release variant, archive the generated mapping file privately for stack-trace de-obfuscation rather than publishing it with the app.

## What should be attached to a tagged release

The exact GitHub workflow must remain the source of truth for automated release assets. From the current repository tooling, the public server asset is expected to be the unified:

```text
sparrow-server.zip
```

plus its checksum when produced by release automation. Android APK/AAB assets and checksums should correspond to the exact tagged commit. Do not document or publish the private `server/control-plane-directory` installer as part of the normal public bundle.

## Release verification checklist

Before tagging/publishing:

1. Verify the tag points at the tested release-branch commit.
2. Build/test the Android release variant with the production discovery configuration.
3. Build `dist/sparrow-server.zip` from the same source revision.
4. Inspect the ZIP to ensure no deployment secrets, identities, Firebase credentials, `.env.runtime`, or `server/control-plane-directory` server/admin files are present.
5. Verify server image tags/digests referenced by the released runtime are immutable/reproducible for that release.
6. Back up production server state before applying new backend images or migrations.
7. Keep release APK mapping/signing material private and backed up.
8. Verify checksums after artifact upload/download.

See [Server runtime, build and deployment](../server/runtime-build-deployment.md) for the operational model implemented by the current scripts.
