# First build

## 1. Make sure `local.properties` contains the directory URL

```properties
controlPlaneDirectoryUrl=https://example.com/control-planes.json
```

The URL must return JSON containing `controlPlanes`. It can be served as `text/plain`.

## 2. Build Android

Windows:

```text
gradlew.bat :androidApp:assembleDebug
```

macOS/Linux:

```bash
./gradlew :androidApp:assembleDebug
```

The APK is written below:

```text
androidApp/build/outputs/apk/debug/
```

The Android application ID is `com.cbgm.sparrow`. For FCM/background wake-ups, `androidApp/google-services.json` must belong to a Firebase Android app registered with that package name. A debug build can still be compiled without the file because the Google Services plugin is applied only when the file exists.

## 3. Run checks

```bash
./gradlew qualityCheck
./gradlew allTests
```

On Windows use `gradlew.bat` if your shell does not execute `./gradlew`.

## 4. Android device tests

Start an emulator/device, then:

```bash
./gradlew connectedCheck
```

## 5. Start servers

For a realistic local setup, run one Control Plane and at least one Community Node. See
[Local development](../development/local-development.md).

## 6. Run the Android app

Run `androidApp` from Android Studio. On startup:

1. `SparrowApplication` starts Android Koin wiring and the shared `App()` shell.
2. `StartupRoute` / `StartupViewModel` invokes `ApplicationStartupRunner.run()`.
3. registered waiting `StartupTask`s initialize language/crypto, load Control Plane configuration, resolve identity status and complete the other startup gates concurrently.
4. `StartupViewModel.completeStartup()` navigates to `AppRoute.Main` and calls `ApplicationStartupRunner.startPostNavigationRuntime()`.
5. non-waiting startup tasks, `ForegroundRuntimeCoordinator` and the transport/runtime observers continue after navigation; a compatible Community Node is then connected through `/v1/gateway`.

## iOS note

A shared iOS framework and Xcode host exist, but this is **not a usable first-build path**. Important platform
implementations and runtime wiring are still incomplete. Do not use an iOS build as a feature-validation target.

## Build cache and generated architecture docs

Normal code builds use Gradle's caches. If module dependencies change, regenerate architecture reference:

```bash
./gradlew architectureReport
./gradlew verifyArchitectureReport
```

`docs/generated/` is generated output; do not edit it manually.
