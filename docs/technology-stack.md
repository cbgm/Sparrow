# Technology stack

Versions below are read from the current `gradle/libs.versions.toml` release snapshot.

## Client/shared

| Technology | Version / role |
|---|---|
| Kotlin | 2.4.20 |
| Android Gradle Plugin | 9.4.1 |
| Compose Multiplatform | 1.12.1 |
| Material 3 | 1.12.0-alpha03 |
| Android compile / target SDK | 37 / 37 |
| Android min SDK | 29 |
| Coroutines | 1.11.0 |
| Koin | 4.2.2 |
| Room | 2.8.5 |
| bundled SQLite | 2.7.1 |
| DataStore | 1.2.1 |
| Coil | 3.6.3 |
| Ktor | 3.6.0 |
| kotlinx.serialization | 1.11.0 |
| libsodium bindings | 0.9.5 |
| Navigation Compose | 2.9.2 |
| CameraX | 1.6.2 |
| WorkManager | 2.11.2 |
| Firebase Messaging | 25.1.3 |
| Kermit | 2.2.0 |
| Okio | 3.18.2 |

## Server/build

| Technology | Version / role |
|---|---|
| PostgreSQL JDBC | 42.7.13 |
| HikariCP | 7.1.0 |
| Jedis | 5.2.0 |
| Firebase Admin | 9.10.0 |
| Micrometer | 1.17.1 |
| Logback | 1.6.5 |
| KSP | 2.3.9 |
| Detekt | 2.0.0-alpha.6 |
| ktlint Gradle / engine | 14.2.0 / 1.7.1 |
| BuildKonfig | 0.23.0 |
| AboutLibraries | 15.2.0 |

`:feature:embedding` owns the shared local embedding runtime used by semantic search/safety. `:feature:voice` owns local native voice transcription. `:feature:media` and `:feature:attachments` split media/device selection from encrypted message-part transfer/storage.

The version catalog is authoritative if this page becomes stale.
