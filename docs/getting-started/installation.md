# Installation

This is the shortest supported setup for a new contributor.

## Windows

Install:

1. Git.
2. Android Studio and the Android SDK.
3. JDK 17 for normal local Gradle/Android development.
4. Docker Desktop with Docker Compose 2.24.4 or newer.
5. PowerShell 5.1+ (Windows already includes it; PowerShell 7 is also fine for repository scripts unless the
   script explicitly uses Windows Forms).

Then clone the repository and open it in Android Studio.

## macOS

Install:

1. Git.
2. Android Studio and the Android SDK.
3. JDK 17.
4. Docker Desktop.
5. Xcode only if you want to inspect the unfinished iOS host.

The Android app can be developed normally on macOS. The Community Node release bundle has a macOS/Linux shell
launcher. The Control Plane's friendly GUI bundle is currently Windows-only; on macOS run the Control Plane from
source with Docker Compose.

## `local.properties`

Create `local.properties` in the repository root if Android Studio did not create it already. Keep your normal SDK
entry and add:

```properties
controlPlaneDirectoryUrl=https://gist.githubusercontent.com/cbgm/26bb9651e7d2d3fd464df02e8808387f/raw/522436a432e48b9f53f3210b76278e2217f126f8/gistfile1.txt
```

Example on Windows:

```properties
sdk.dir=C\\:\\Users\\Chris\\AppData\\Local\\Android\\Sdk
controlPlaneDirectoryUrl=https://example.com/control-planes.json
```

Example on macOS:

```properties
sdk.dir=/Users/you/Library/Android/sdk
controlPlaneDirectoryUrl=https://example.com/control-planes.json
```

`local.properties` is local build configuration and should not be committed with machine-specific values.

## Why the directory value is a build variable

The app cannot read the repository's `local.properties` file after installation. Gradle reads it at build time.
The `shared` module's BuildKonfig configuration turns it into:

```kotlin
BuildKonfig.CONTROL_PLANE_DIRECTORY_URL
```

That constant is available to common KMP code, so the startup logic is not Android-specific.

## Verify tools

Windows:

```text
java -version
docker --version
docker compose version
gradlew.bat --version
```

macOS:

```bash
java -version
docker --version
docker compose version
./gradlew --version
```

## Optional documentation tooling

The docs site uses MkDocs Material. CI can build it, but if you want to preview locally, use a Python environment
with MkDocs Material and run:

```bash
mkdocs serve
```

## Next

Continue with [First build](first-build.md).
