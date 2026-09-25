# `:server:gateway`

Source directory: `server/gateway`

## Direct project dependencies

- `:server:persistence`
- `:server:protocol`
- `:server:security`
- `:server:observability`
- `:server:linkPreview`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `BestEffortPresenceClient` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayBackgroundClients.kt` |
| `BlobAlreadyExistsException` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobCleanupAgent` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobCleanupAgent.kt` |
| `BlobStorageCapacityExceededException` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobStore` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobTooLargeException` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobStore.kt` |
| `BlobUploadPermitCleanupAgent` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobUploadPermitCleanupAgent.kt` |
| `BlobUploadPermitStore` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/BlobUploadPermitStore.kt` |
| `ConnectionRegistry` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/ConnectionRegistry.kt` |
| `EnvelopeFallbackActions` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayWebSocketHandler.kt` |
| `FederationClient` | `interface` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |
| `GatewayBlobUploadTicketIssuer` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayBlobUploadTicketIssuer.kt` |
| `GatewayConfig` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/Application.kt` |
| `GatewayConnection` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/ConnectionRegistry.kt` |
| `GatewayControlPlaneDirectory` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayControlPlaneDiscovery.kt` |
| `GatewayControlPlaneDiscovery` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayControlPlaneDiscovery.kt` |
| `GatewayMessageActions` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewayPushActions` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewayPushDispatcher` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayBackgroundClients.kt` |
| `GatewayRouteValidationFailure` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayRouteValidator.kt` |
| `GatewayRouteValidator` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayRouteValidator.kt` |
| `GatewayRuntime` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayRuntime.kt` |
| `GatewaySessionHandler` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewaySessionState` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionHandler.kt` |
| `GatewaySessionWorkDispatcher` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewaySessionWorkDispatcher.kt` |
| `GatewayWebSocketHandler` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayWebSocketHandler.kt` |
| `HttpFederationClient` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `HttpLegacyPushClient` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `HttpNodePushClient` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `HttpPresenceClient` | `class` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/HttpGatewayClients.kt` |
| `LegacyPushClient` | `interface` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |
| `PresenceClient` | `interface` | `main` | `server/gateway/src/main/kotlin/com/cbgm/sparrow/server/gateway/GatewayPorts.kt` |
