# `:server:federation`

Source directory: `server/federation`

## Direct project dependencies

- `:server:protocol`
- `:server:security`
- `:server:persistence`
- `:server:observability`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `CachingNodeRegistryClient` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/CachingNodeRegistryClient.kt` |
| `FederationConfig` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/Application.kt` |
| `FederationPeerRouter` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationPeerRouter.kt` |
| `FederationRouter` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationRouter.kt` |
| `FederationRuntime` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationRuntime.kt` |
| `HttpGatewayLoadProvider` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `HttpLocalGatewayClient` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `HttpMailboxClient` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `HttpPresenceDirectoryClient` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `HttpRemoteFederationClient` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/HttpFederationClients.kt` |
| `ManagedHttpClient` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/FederationRuntime.kt` |
| `NodeRegistrationAgent` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `NodeRegistrationClient` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `NodeRegistrationConfig` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/NodeRegistrationAgent.kt` |
| `OutboundEnvelopeEntry` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeQueue.kt` |
| `OutboundEnvelopeQueue` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeQueue.kt` |
| `OutboundEnvelopeRetryAgent` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeRetryAgent.kt` |
| `OutboundEnvelopeStorage` | `interface` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/OutboundEnvelopeQueue.kt` |
| `PostgresOutboundEnvelopeDatabase` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeDatabase.kt` |
| `PostgresOutboundEnvelopeDatabaseConfig` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeDatabase.kt` |
| `PostgresOutboundEnvelopeStorage` | `class` | `main` | `server/federation/src/main/kotlin/com/cbgm/sparrow/server/federation/PostgresOutboundEnvelopeStorage.kt` |
