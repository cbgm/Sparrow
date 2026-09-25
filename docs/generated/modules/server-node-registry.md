# `:server:node-registry`

Source directory: `server/node-registry`

## Direct project dependencies

- `:server:protocol`
- `:server:security`
- `:server:persistence`
- `:server:observability`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `CertifiedRegistrySigner` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `DirectRegistryDirectorySigner` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `NodeRegistryConfig` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/Application.kt` |
| `NodeRegistryStorage` | `interface` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `NodeRegistryStore` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `PostgresNodeRegistryDatabase` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryDatabase.kt` |
| `PostgresNodeRegistryDatabaseConfig` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryDatabase.kt` |
| `PostgresNodeRegistryStore` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/PostgresNodeRegistryStore.kt` |
| `RegistrationResult` | `interface` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/NodeRegistryStore.kt` |
| `RegistryAuthorityCertificateStore` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryAuthorityCertificateStore.kt` |
| `RegistryAuthorityProvisioningCli` | `object` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryAuthorityProvisioningCli.kt` |
| `RegistryDirectorySigner` | `interface` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `RegistrySigningConfig` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
| `RegistrySigningRuntime` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistrySigningRuntime.kt` |
| `RotatingRegistryDirectorySigner` | `class` | `main` | `server/node-registry/src/main/kotlin/com/cbgm/sparrow/server/registry/RegistryDirectorySigner.kt` |
