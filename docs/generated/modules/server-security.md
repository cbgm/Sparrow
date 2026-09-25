# `:server:security`

Source directory: `server/security`

## Direct project dependencies

- `:server:protocol`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `BoundedRateLimiter` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `ClientRateLimitKeys` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `ClientRoutingIds` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/ClientRoutingIds.kt` |
| `CommandLineOutput` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/CommandLineOutput.kt` |
| `InternalApiAuthentication` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/InternalApiAuthentication.kt` |
| `NodeIdentity` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeIdentity.kt` |
| `NodeIdentityStore` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeIdentityStore.kt` |
| `NodeIds` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeIds.kt` |
| `NodeRequestAuthentication` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeRequestAuthorizationRequirements` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthorizer.kt` |
| `NodeRequestAuthorizer` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthorizer.kt` |
| `NodeRequestHeaders` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeRequestSignatureCli` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestSignatureCli.kt` |
| `NodeRequestSigner` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `NodeRequestVerifier` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/NodeRequestAuthentication.kt` |
| `PresenceRouteRegistrationCli` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/PresenceRouteRegistrationCli.kt` |
| `ProtocolSignatures` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/ProtocolSignatures.kt` |
| `RateLimitDecision` | `interface` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `RateLimitPolicy` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RequestRateLimiting.kt` |
| `RegistryCertificateSignatures` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/RegistryCertificateSignatures.kt` |
| `ReplayProtection` | `class` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/ReplayProtection.kt` |
| `Signatures` | `object` | `main` | `server/security/src/main/kotlin/com/cbgm/sparrow/server/security/Signatures.kt` |
