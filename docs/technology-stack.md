# Technology stack

This page reflects the versions in the current `gradle/libs.versions.toml` snapshot.

## Client / shared

| Technology | Current version / role |
|---|---|
| Kotlin | 2.4.0 |
| Compose Multiplatform | 1.11.1 |
| Android Gradle Plugin | 9.3.2 |
| Android compile / target SDK | 37 / 37 |
| Android min SDK | 29 |
| Coroutines | 1.11.0 |
| Koin | 4.1.1 |
| Room | 2.8.4 |
| SQLite bundled | 2.7.0 |
| DataStore | 1.2.1 |
| Coil | 3.5.0 |
| Ktor | 3.5.1 |
| kotlinx.serialization | 1.9.0 |
| libsodium bindings | 0.9.5 |
| Navigation Compose | 2.9.2 |
| CameraX | 1.5.1 |
| WorkManager | 2.11.2 |
| Firebase Messaging | 25.0.1 |
| Kermit logging | 2.1.0 |

## Server

Server services are Kotlin/JVM/Ktor applications packaged into Docker images behind Caddy.

| Technology | Current version / role |
|---|---|
| Ktor server/client | 3.5.1 |
| PostgreSQL driver | 42.7.13 |
| HikariCP | 7.1.0 |
| Jedis | 5.2.0 |
| Firebase Admin | 9.10.0 |
| Micrometer | 1.17.0 |
| Logback | 1.5.18 |
| PostgreSQL container | 17-alpine in current Compose files |
| Redis container | 7.4-alpine in Control Plane Compose |
| Caddy | 2-alpine in deployment Compose files |

## Local AI / media

- `:core:embedding` supplies the shared local embedding model/runtime used by semantic search and message safety.
- `:feature:voice` uses local Whisper-native integration (`WhisperNative`, `AndroidWhisperModelStore`, `AndroidVoiceTranscriptionRepository`) for voice transcription.
- Camera/gallery/file/media work is split between `:feature:media`, `:feature:attachments` and platform source sets.

## Build/quality

- KSP 2.3.9
- Detekt 2.0.0-alpha.5
- ktlint Gradle plugin 14.2.0 / engine 1.7.1
- BuildKonfig 0.22.0
- AboutLibraries plugin 15.0.4

The version catalog is the source of truth when this page and code disagree.
