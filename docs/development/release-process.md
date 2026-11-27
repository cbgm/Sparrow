# Release process

This is the recommended open-source workflow for Sparrow's `v0.6.0` beta, release candidates,
final release, and later hotfixes. Keep `master` stable, use `develop` for ongoing work, and keep
one `release/0.6` branch for the entire 0.6.0 lifecycle. GitHub Actions creates a release build
from each pushed `v*` tag.

## 1. Branch structure

| Branch | Purpose |
|---|---|
| `master` | Stable published releases and open-source preparation needed before publication |
| `develop` | Ongoing feature development and integration |
| `release/0.6` | All 0.6.0 beta, release-candidate, and final-release preparation |
| `feature/*` | New features; normally merge into `develop` |
| `bugfix/*` | Normal bug fixes, branched from the line being fixed |
| `hotfix/*` | Urgent fixes to a published release, branched from `master` |

Use `release/0.6` for `v0.6.0-*` tags. The release workflow also accepts a versioned branch such as
`release/0.6.0`, but do not create a second release branch for the same release line.

## 2. Initial open-source preparation

If the license and open-source preparation changes are already merged into `develop`, identify
only those commit(s) and cherry-pick them into `master`. This publishes the licensing and project
community files without bringing unfinished development into the stable branch.

```powershell
git checkout develop
git pull origin develop
git log --oneline

git checkout master
git pull origin master
git cherry-pick <license-commit-hash>
git push origin master
```

Repeat `git cherry-pick <commit-hash>` for each required preparation commit. Review each commit
before cherry-picking; do not cherry-pick unrelated feature work. Resolve any conflicts, run the
relevant checks, and confirm the resulting `master` contains the intended license and community
files.

## 3. Create the release branch

Start the release branch at the updated stable `master`, then merge the current development state
to create the initial 0.6.0 candidate:

```powershell
git checkout master
git pull origin master

git checkout -b release/0.6
git push -u origin release/0.6

git merge develop --no-ff
git push origin release/0.6
```

Resolve merge conflicts deliberately and test the resulting tree. Do not merge unfinished
development into `master`; the release branch is where `develop` is stabilized.

## 4. Beta and release-candidate cycle

Keep using `release/0.6` for all 0.6.0 releases:

| Stage | Example tag | Purpose |
|---|---|---|
| Beta 1 | `v0.6.0-beta.1` | Initial public testing build |
| Beta 2 | `v0.6.0-beta.2` | Bug fixes and improvements |
| Beta 3+ | `v0.6.0-beta.3` | Further testing and fixes as needed |
| Release candidate | `v0.6.0-rc.1` | Final validation and stability checks |
| Final | `v0.6.0` | Stable public release |

After the commit is ready and tested on the release branch, tag that commit and push the tag:

```powershell
git checkout release/0.6
git pull origin release/0.6
git status
git tag -a v0.6.0-beta.1 -m "Sparrow v0.6.0 Beta 1"
git push origin v0.6.0-beta.1
```

Use a new tag for every beta or release candidate, changing the version and message as appropriate.
The tag identifies the complete build, including all fixes merged before it. Do not tag an
individual bugfix branch. The release workflow checks out the exact tagged commit, builds its
release assets, and marks version tags containing a suffix (such as `-beta.1` or `-rc.1`) as
pre-releases on GitHub.

## 5. Bug fixes during the release

Branch bug fixes from the release line they are intended to fix. For a 0.6.0 beta:

```powershell
git checkout release/0.6
git pull origin release/0.6
git checkout -b bugfix/registration-error

# Implement and test the fix.
git add .
git commit -m "fix: resolve registration error"
git push -u origin bugfix/registration-error
```

Merge the fix into the release line and push it:

```powershell
git checkout release/0.6
git pull origin release/0.6
git merge bugfix/registration-error --no-ff
git push origin release/0.6
```

Tag and push the next beta (for example `v0.6.0-beta.2`) from the updated release branch. The
new tag, not a separate bugfix tag, identifies the complete build containing the fix.

### Keep `develop` up to date

After merging a release-specific fix, also merge it into ongoing development so it is not lost:

```powershell
git checkout develop
git pull origin develop
git merge bugfix/registration-error --no-ff
git push origin develop
```

If the fix is already in `develop`, do not merge it again. Delete the bugfix branch when it is no
longer needed.

## 6. Final release

Once the release candidate has passed testing, tag the final commit on `release/0.6`:

```powershell
git checkout release/0.6
git pull origin release/0.6
git status
git tag -a v0.6.0 -m "Sparrow v0.6.0"
git push origin v0.6.0
```

After the tagged release succeeds, merge the finalized release line into both stable and ongoing
development:

```powershell
git checkout master
git pull origin master
git merge release/0.6 --no-ff
git push origin master

git checkout develop
git pull origin develop
git merge release/0.6 --no-ff
git push origin develop
```

Delete `release/0.6` after the release is complete if no further 0.6.x maintenance is planned.

## 7. Hotfixes after the final release

For an urgent fix to the published `v0.6.0`, branch from stable `master`:

```powershell
git checkout master
git pull origin master
git checkout -b hotfix/critical-security-fix

# Implement and test the fix.
git add .
git commit -m "fix: address critical security issue"
git push -u origin hotfix/critical-security-fix
```

Merge the fix into `master`, then tag and push the patch release from the updated stable branch:

```powershell
git checkout master
git pull origin master
git merge hotfix/critical-security-fix --no-ff
git push origin master
git tag -a v0.6.1 -m "Sparrow v0.6.1"
git push origin v0.6.1
```

Also merge the fix into `develop`:

```powershell
git checkout develop
git pull origin develop
git merge hotfix/critical-security-fix --no-ff
git push origin develop
```

Stable release tags can point to commits on `master` or `release/*`; beta and RC tags must point
to commits on `release/*`. This lets the patch tag identify the exact tested hotfix on `master`
while preserving the release workflow's branch validation.

## 8. GitHub Actions and GitHub Releases

The release workflow listens for all version tags:

```yaml
on:
  push:
    tags:
      - "v*"
```

It checks out the tagged commit itself, not `master`. Beta and RC tags (version strings containing
a hyphen) are published as GitHub pre-releases. Final and patch tags without a suffix are
published as stable releases. Pushes to `release/0.6` do not themselves create the tagged GitHub
release; push a new version tag to build and publish each beta, RC, final, or patch release.

## 9. Workflow at a glance

```text
master
  open-source preparation -> stable releases and hotfixes
       │                         ▲
       └── create release/0.6 ───┘ merge final release
                    │
develop ── merge ──> release/0.6
                       beta tags -> RC tag -> v0.6.0
                    │
                    └── merge release fixes back into develop

After v0.6.0:
hotfix/* from master -> merge to master -> v0.6.1 tag
                                  └──────> merge fix to develop
```

## Recommendations

1. Keep `master` stable; do not merge unfinished `develop` work into it.
2. Use one `release/0.6` branch for the complete 0.6.0 beta, RC, and final cycle.
3. Give every beta and RC a new tag; each tag builds the complete release.
4. Branch bug fixes from the line they are fixing.
5. Merge release-specific fixes back to `develop`.
6. Never tag a bugfix branch just to trigger a build.
7. Branch urgent post-release hotfixes from `master`, and merge their fixes into `develop`.

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

## Release automation present in this source snapshot

The workflows are checked in under `.github/workflows`:

| Workflow | Trigger | Purpose |
|---|---|---|
| `android-pr.yml` | pull requests to `develop`, manual | Android build and checks |
| `server-smoke.yml` | pull requests to `develop` touching server/build files | Server smoke test |
| `server-community-node-smoke.yml` | pull requests to `develop` touching server/build files | Community Node smoke test |
| `sparrow-server-bundle.yml` | pushes touching server/build files | Server bundle artifacts |
| `docs.yml` | pushes to `master` touching `docs/**` or `mkdocs.yml` | Documentation site |
| `release.yml` | `v*` tags | Full release build and GitHub Release |

If a workflow changes, keep this page synchronized with the checked-in workflow files.

## Current public server bundle

The current server distribution path is a **single unified server bundle**, not separate public Control Plane and Community Node packages.

Build from the repository root (Windows, Linux or macOS):

```text
python3 server/installer/build.py
```

This produces:

```text
dist/sparrow-server.zip
```

The bundle contains the cross-platform runtime/manager scripts plus the Control Plane and Community Node Compose/Caddy configuration needed by the unified manager. It does not contain live deployment secrets or state.

Important bundled entry points include:

- `Start-SparrowServer.cmd`, `Start-SparrowServer.sh`, `Start-SparrowServer.command` (top level)
- `app/` with the manager UI, `Invoke-SparrowServer.ps1` / `.py`, `Update-SparrowServer.ps1`,
  `Manage-SparrowNodes.py`, the installer window (`manager_window.py`) and `bundle.json` (source repository and image tag)
- the Control Plane and Community Node Compose/Caddy templates
- signed-directory client/registration helpers used by the public runtime
- shared-public-proxy definitions used by public Combined deployments

All three launchers open the same cross-platform installer window (Tk, shared `ui_window.py`); without a desktop the same options appear as a text menu.

## Server bundle exclusion: independent Control Plane Directory

`server/control-plane-directory` is public source for a separate operator-managed service. `server/installer/build.py` deliberately excludes its service and installer from the unified runtime bundle; generated server identities and secrets must never be copied into release assets.

Therefore these are **not public Sparrow server release assets**:

- the independent directory's separately built operator installer;
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

plus its checksum when produced by release automation. Android APK/AAB assets and checksums should correspond to the exact tagged commit. Do not document or publish the separately built `server/control-plane-directory` installer as part of the normal unified runtime bundle.

## Release verification checklist

Before tagging/publishing:

1. Verify beta/RC tags point at tested `release/*` commits and stable tags point at a tested `master` or `release/*` commit.
2. Build/test the Android release variant with the production discovery configuration.
3. Build `dist/sparrow-server.zip` from the same source revision.
4. Inspect the ZIP to ensure no deployment secrets, identities, Firebase credentials, `.env.runtime`, or `server/control-plane-directory` server/admin files are present.
5. Verify server image tags/digests referenced by the released runtime are immutable/reproducible for that release.
6. Back up production server state before applying new backend images or migrations.
7. Keep release APK mapping/signing material private and backed up.
8. Verify checksums after artifact upload/download.

See [Server runtime, build and deployment](../server/runtime-build-deployment.md) for the operational model implemented by the current scripts.
