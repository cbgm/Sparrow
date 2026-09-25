# `:server:mailbox`

Source directory: `server/mailbox`

## Direct project dependencies

- `:server:protocol`
- `:server:security`
- `:server:persistence`
- `:server:observability`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `InMemoryMailbox` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxAuthorization` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxQueries.kt` |
| `MailboxConfig` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/Application.kt` |
| `MailboxCreationResult` | `interface` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxCredentials` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxStore.kt` |
| `MailboxPushNotifier` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxPushNotifier.kt` |
| `MailboxResult` | `interface` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxRevocationResult` | `interface` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxStorage` | `interface` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `MailboxStore` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/MailboxStore.kt` |
| `PostgresMailboxDatabase` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxDatabase.kt` |
| `PostgresMailboxDatabaseConfig` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxDatabase.kt` |
| `PostgresMailboxQueries` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxQueries.kt` |
| `PostgresMailboxStore` | `class` | `main` | `server/mailbox/src/main/kotlin/com/cbgm/sparrow/server/mailbox/PostgresMailboxStore.kt` |
