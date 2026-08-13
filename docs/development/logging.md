# Logging

SecureChat uses a project-owned logging boundary for application/shared Kotlin code and JVM-native
SLF4J logging for server services.

## Client/shared code

The shared API lives under:

```text
core/src/commonMain/kotlin/com/cbgm/securechat/core/logging/
```

Use `SecureChatLog.withTag(tag)` and lazy log-message lambdas. Feature modules should depend on the
SecureChat logging API rather than a concrete logging vendor.

```kotlin
private val logger = SecureChatLog.withTag("DefaultIncomingEnvelopeRunner")

logger.debug { "Incoming envelope acknowledged: envelopeId=$envelopeId" }
logger.warn(error) { "Typing state could not be sent" }
logger.error(error) { "Incoming envelope processing failed" }
```

## Server code

The services under `server/` use SLF4J/Logback through their Ktor/JVM environment. For example:

```kotlin
private val logger = LoggerFactory.getLogger(GatewayWebSocketHandler::class.java)
```

There is no standalone `:server:gateway` logger/runtime anymore.

## Levels

| Level | Use |
|---|---|
| `debug` | Packet progress, receipts, connection attempts, retry timing |
| `info` | Successful connection, disconnection, startup and synchronization milestones |
| `warn` | Recoverable protocol/transport conditions, ignored input, missing permissions |
| `error` | Failed operations that need investigation; include the original `Throwable` |

Do not log expected control flow as an error.

## Privacy

Never log message content, phone numbers/display names, private/public/session/group keys, safety
numbers, signatures, invitation challenges, QR payloads, or encoded protocol/transport payloads.
Identifiers should be logged only when needed to correlate a failure.

## Enforcement

Detekt rejects direct console printing and `Throwable.printStackTrace()`. Run:

```bash
./gradlew qualityCheck
```

before committing.

Crash reporting, file logging or telemetry should remain behind the shared client logging boundary.
Server services may evolve their structured SLF4J pipeline independently.
