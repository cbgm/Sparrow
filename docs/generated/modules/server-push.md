# `:server:push`

Source directory: `server/push`

## Direct project dependencies

- `:server:protocol`
- `:server:persistence`
- `:server:security`
- `:server:observability`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `FirebasePushSender` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/FirebasePushSender.kt` |
| `InMemoryPendingEnvelopeStore` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `InMemoryPushDeviceStore` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `InMemoryWakeUpStore` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PendingEnvelopeStore` | `interface` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PostgresPendingEnvelopeStore` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPendingEnvelopeStore.kt` |
| `PostgresPushDatabase` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPushDatabase.kt` |
| `PostgresPushDatabaseConfig` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPushDatabase.kt` |
| `PostgresPushDeviceStore` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresPushDeviceStore.kt` |
| `PostgresWakeUpStore` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PostgresWakeUpStore.kt` |
| `PushConfig` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/Application.kt` |
| `PushCoordinator` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushCoordinator.kt` |
| `PushDevice` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PushDeviceStore` | `interface` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `PushNodeApiRuntime` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushRuntime.kt` |
| `PushRuntime` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushRuntime.kt` |
| `PushStores` | `class` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
| `WakeUpStore` | `interface` | `main` | `server/push/src/main/kotlin/com/cbgm/sparrow/server/push/PushStores.kt` |
