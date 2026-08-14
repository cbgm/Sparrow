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

The debug APK uses `CONTROL_PLANE_DIRECTORY_URL`. The signed release APK uses
`CONTROL_PLANE_RELEASE_DIRECTORY_URL`. Both are written into CI `local.properties` as:

```properties
controlPlaneDirectoryUrl=...
```

and become common KMP `BuildKonfig.CONTROL_PLANE_DIRECTORY_URL`.

## Android signing secrets

Configure GitHub Actions **Secrets**:

```text
ANDROID_RELEASE_KEYSTORE_BASE64
ANDROID_RELEASE_KEYSTORE_PASSWORD
ANDROID_RELEASE_KEY_ALIAS
ANDROID_RELEASE_KEY_PASSWORD
```

`ANDROID_RELEASE_KEYSTORE_BASE64` is the Base64-encoded `.jks` file, not a password. Keep the original keystore in
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

Paste that file's contents into `ANDROID_RELEASE_KEYSTORE_BASE64`. The other three secrets contain the actual
keystore password, alias and key password. Back up the original `.jks` offline; do not commit it.

## Create a release branch

When `master` is ready to stabilize a release line:

```bash
git switch master
git pull
git switch -c release/0.1
git push -u origin release/0.1
```

Every later push to that branch creates/updates release-candidate artifacts according to the changed files below.
It does **not** publish the official GitHub Release yet.

## Every push to `release/**`

`.github/scripts/resolve-release-changes.sh` classifies the diff.

| Changed files | Artifacts |
|---|---|
| client/shared/features/resources | debug APK + signed release APK |
| `server/node-registry/**` | node-registry image + Control Plane bundle |
| `server/presence-directory/**` | presence image + Control Plane bundle |
| `server/push/**` | push image + Control Plane bundle |
| `server/gateway/**` | gateway image + Community Node bundle |
| `server/federation/**` | federation image + Community Node bundle |
| `server/mailbox/**` | mailbox image + Community Node bundle |
| shared server protocol/security/persistence/observability | all server images + both bundles |
| Control Plane launcher/Caddy/compose only | Control Plane bundle only |
| Community Node launcher/Caddy/compose only | Community Node bundle only |
| docs only | validation only; no distributable artifact |
| build/release infrastructure | conservative full artifact build |

The first push of a new release line has no useful previous release-line SHA and therefore bootstraps a full build.

Unchanged server images are not unnecessarily rebuilt. The workflow can copy existing image manifests to the new
immutable candidate tag so a launcher bundle still references one reproducible tag across all services.

## Candidate versions

A branch such as `release/0.1` produces versions similar to:

```text
0.1-rc.<github-run-number>-<short-sha>
```

and immutable image tags similar to:

```text
release-0-1-sha-<short-sha>
```

A moving `release-0-1` image tag represents the current release line; immutable candidate tags preserve exact
candidate contents.

## What builds in common examples

App-only change (`feature/chats/**`, `shared/**`, etc.):

```text
✓ debug APK
✓ signed release APK
✗ server image rebuilds
✗ unrelated launcher bundles
```

Push-service-only change:

```text
✓ push image
✓ Control Plane bundle that references the new candidate tag
✗ Android APKs
✗ Community Node bundle
```

Community Node Caddy/launcher-only change:

```text
✓ Community Node bundle
✗ gateway/federation/mailbox image rebuilds
✗ Android APKs
```

A shared server-security/protocol change conservatively rebuilds all server images and both server bundles.

## Tagging a full release

Tag an exact commit that belongs to a `release/**` branch:

```bash
git tag v0.1.0-alpha.1
git push origin v0.1.0-alpha.1
```

Stable format also works:

```text
v0.1.0
```

A `v*` tag **forces a complete build**, regardless of change detection:

- debug APK;
- signed release APK;
- all server images;
- Control Plane launcher package;
- Community Node Windows package;
- Community Node macOS/Linux package;
- checksums and release metadata;
- combined full ZIP;
- GitHub Release/Pre-Release.

Tags with a suffix such as `-alpha.1` are published as prereleases. A plain semantic version is published as a
normal release. The workflow verifies that the tagged commit belongs to a `release/**` branch; do not tag an arbitrary
feature/develop/master commit and expect it to publish.

After the workflow succeeds, open the repository's **Releases** page. GitHub shows the individual assets plus the
combined full ZIP. GitHub also adds its normal source-code ZIP/tarball automatically; those source archives are not the
same thing as Sparrow's packaged `sparrow-<version>-full.zip`.

## Full ZIP

The GitHub release includes individual assets and:

```text
sparrow-<version>-full.zip
```

Conceptually:

```text
Sparrow-<version>/
├── app/
│   ├── sparrow-<version>-debug.apk
│   └── sparrow-<version>-release.apk
├── control-plane/
│   └── sparrow-control-plane-<version>-windows.zip
├── community-node/
│   ├── sparrow-community-node-<version>-windows.zip
│   └── sparrow-community-node-<version>-macos-linux.tar.gz
├── RELEASE.txt
├── MANIFEST.txt
└── SHA256SUMS.txt
```

The outer GitHub release also has checksums including the full ZIP itself.

## Release APK optimization

Release builds use:

```text
R8 minification = enabled
resource shrinking = enabled
optimized default ProGuard configuration
```

Debug builds remain unminified.

`mapping.txt` is uploaded as a private GitHub Actions artifact for 90 days so obfuscated stack traces can be
de-obfuscated. It is deliberately not published in the public release/full ZIP.

## Docker images and launcher packages

Launcher bundles contain Compose/Caddy/config/launcher files, not six duplicated server binaries. They pull the
versioned images from GHCR. Full release tags therefore bind launcher packages to exact server image versions.
