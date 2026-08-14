package com.cbgm.sparrow.server.registry

import com.cbgm.sparrow.server.observability.installServerObservability
import com.cbgm.sparrow.server.persistence.ServiceEnvironment
import com.cbgm.sparrow.server.protocol.ErrorResponse
import com.cbgm.sparrow.server.protocol.NodeDirectory
import com.cbgm.sparrow.server.protocol.NodeHeartbeatRequest
import com.cbgm.sparrow.server.protocol.NodeRegistrationRequest
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.BoundedRateLimiter
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.RateLimitPolicy
import com.cbgm.sparrow.server.security.enforceRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

private const val DEFAULT_NODE_REGISTRY_PORT = 8090

fun main() {
    val signingRuntime = createRegistrySigningRuntime()

    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = ServiceEnvironment.int("PORT", DEFAULT_NODE_REGISTRY_PORT),
        module = {
            nodeRegistryModule(
                identity = signingRuntime.identity,
                directorySigner = signingRuntime.directorySigner
            )
        }
    ).start(wait = true)
}

fun Application.nodeRegistryModule(
    identity: NodeIdentity,
    config: NodeRegistryConfig = NodeRegistryConfig.fromEnvironment(),
    store: NodeRegistryStorage = createNodeRegistryStorage(config),
    directorySigner: RegistryDirectorySigner = DirectRegistryDirectorySigner(identity)
) {
    val registrationRateLimiter = BoundedRateLimiter(config.registrationRateLimit)
    val heartbeatRateLimiter = BoundedRateLimiter(config.heartbeatRateLimit)
    monitor.subscribe(ApplicationStopped) {
        store.close()
    }

    installServerObservability("node-registry") {
        store.healthyNodes()
        true
    }
    install(ContentNegotiation) { json(serverJson) }
    if (config.trustProxyHeaders) {
        install(XForwardedHeaders)
    }

    routing {
        get("/health") {
            call.respondText(
                "ok persistence=${store.persistenceMode} nodes=${store.healthyNodes().size}"
            )
        }

        post("/v1/nodes") {
            val descriptor = call.receive<NodeRegistrationRequest>().descriptor
            if (!call.enforceRateLimit(registrationRateLimiter, descriptor.nodeId)) {
                return@post
            }

            when (val result = store.register(descriptor)) {
                RegistrationResult.Accepted -> call.respond(HttpStatusCode.Created)
                is RegistrationResult.Rejected ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(result.code, "Node registration rejected")
                    )
            }
        }

        post("/v1/nodes/{nodeId}/heartbeat") {
            val nodeId = call.parameters["nodeId"].orEmpty()
            if (!call.enforceRateLimit(heartbeatRateLimiter, nodeId)) {
                return@post
            }

            val heartbeat = call.receive<NodeHeartbeatRequest>()
            if (heartbeat.nodeId != nodeId) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("NODE_ID_MISMATCH", "Path and body differ")
                )
                return@post
            }

            when (val result = store.heartbeat(heartbeat)) {
                RegistrationResult.Accepted -> call.respond(HttpStatusCode.NoContent)
                is RegistrationResult.Rejected ->
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse(result.code, "Heartbeat rejected")
                    )
            }
        }

        get("/v1/nodes") {
            val generatedAt = System.currentTimeMillis()
            val directory =
                NodeDirectory(
                    generatedAtEpochMilliseconds = generatedAt,
                    validUntilEpochMilliseconds = generatedAt + 60_000L,
                    nodes = store.healthyNodes()
                )
            call.respond(directorySigner.sign(directory))
        }

        get("/v1/nodes/{nodeId}") {
            val nodeId = call.parameters["nodeId"]
            val descriptor = if (nodeId == null) null else store.findHealthy(nodeId)
            if (descriptor == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(descriptor)
            }
        }
    }
}

data class NodeRegistryConfig(
    val databaseUrl: String?,
    val databaseUser: String,
    val databasePassword: String,
    val databaseMaximumPoolSize: Int,
    val supportedProtocolVersions: Set<Int>,
    val heartbeatGraceMilliseconds: Long,
    val replayRetentionMilliseconds: Long,
    val registrationRateLimit: RateLimitPolicy =
        RateLimitPolicy(
            maximumRequests = DEFAULT_REGISTRATION_RATE_LIMIT_REQUESTS,
            windowMilliseconds = DEFAULT_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS
        ),
    val heartbeatRateLimit: RateLimitPolicy =
        RateLimitPolicy(
            maximumRequests = DEFAULT_HEARTBEAT_RATE_LIMIT_REQUESTS,
            windowMilliseconds = DEFAULT_HEARTBEAT_RATE_LIMIT_WINDOW_MILLISECONDS
        ),
    val trustProxyHeaders: Boolean = false
) {
    init {
        require(databaseMaximumPoolSize > 0) {
            "Node registry database maximum pool size must be positive"
        }
        require(supportedProtocolVersions.isNotEmpty() && supportedProtocolVersions.all { it > 0 }) {
            "Supported protocol versions must be positive"
        }
        require(heartbeatGraceMilliseconds > 0L) {
            "Heartbeat grace period must be positive"
        }
        require(replayRetentionMilliseconds > 0L) {
            "Replay retention must be positive"
        }
    }

    companion object {
        fun fromEnvironment(): NodeRegistryConfig =
            NodeRegistryConfig(
                databaseUrl =
                    System.getenv("NODE_REGISTRY_DATABASE_URL")?.takeIf(String::isNotBlank),
                databaseUser = System.getenv("NODE_REGISTRY_DATABASE_USER").orEmpty(),
                databasePassword =
                    ServiceEnvironment.secret("NODE_REGISTRY_DATABASE_PASSWORD").orEmpty(),
                databaseMaximumPoolSize =
                    System.getenv("NODE_REGISTRY_DATABASE_MAXIMUM_POOL_SIZE")?.toIntOrNull()
                        ?: DEFAULT_DATABASE_MAXIMUM_POOL_SIZE,
                supportedProtocolVersions = setOf(1),
                heartbeatGraceMilliseconds =
                    System.getenv("NODE_REGISTRY_HEARTBEAT_GRACE_MILLISECONDS")?.toLongOrNull()
                        ?: DEFAULT_HEARTBEAT_GRACE_MILLISECONDS,
                replayRetentionMilliseconds =
                    System.getenv("NODE_REGISTRY_REPLAY_RETENTION_MILLISECONDS")?.toLongOrNull()
                        ?: DEFAULT_REPLAY_RETENTION_MILLISECONDS,
                registrationRateLimit =
                    RateLimitPolicy(
                        maximumRequests =
                            ServiceEnvironment.int(
                                "NODE_REGISTRY_REGISTRATION_RATE_LIMIT_REQUESTS",
                                DEFAULT_REGISTRATION_RATE_LIMIT_REQUESTS
                            ),
                        windowMilliseconds =
                            ServiceEnvironment.long(
                                "NODE_REGISTRY_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS",
                                DEFAULT_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS
                            ),
                        maximumTrackedClients =
                            ServiceEnvironment.int(
                                "NODE_REGISTRY_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS",
                                DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS
                            )
                    ),
                heartbeatRateLimit =
                    RateLimitPolicy(
                        maximumRequests =
                            ServiceEnvironment.int(
                                "NODE_REGISTRY_HEARTBEAT_RATE_LIMIT_REQUESTS",
                                DEFAULT_HEARTBEAT_RATE_LIMIT_REQUESTS
                            ),
                        windowMilliseconds =
                            ServiceEnvironment.long(
                                "NODE_REGISTRY_HEARTBEAT_RATE_LIMIT_WINDOW_MILLISECONDS",
                                DEFAULT_HEARTBEAT_RATE_LIMIT_WINDOW_MILLISECONDS
                            ),
                        maximumTrackedClients =
                            ServiceEnvironment.int(
                                "NODE_REGISTRY_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS",
                                DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS
                            )
                    ),
                trustProxyHeaders =
                    ServiceEnvironment.string("TRUST_PROXY_HEADERS", "false").toBoolean()
            )

        private const val DEFAULT_DATABASE_MAXIMUM_POOL_SIZE = 10
        private const val DEFAULT_HEARTBEAT_GRACE_MILLISECONDS = 30_000L
        private const val DEFAULT_REPLAY_RETENTION_MILLISECONDS = 5L * 60L * 1_000L
        private const val DEFAULT_REGISTRATION_RATE_LIMIT_REQUESTS = 30
        private const val DEFAULT_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS = 60L * 60L * 1_000L
        private const val DEFAULT_HEARTBEAT_RATE_LIMIT_REQUESTS = 180
        private const val DEFAULT_HEARTBEAT_RATE_LIMIT_WINDOW_MILLISECONDS = 60_000L
        private const val DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS = 100_000
    }
}

internal fun createNodeRegistryStorage(config: NodeRegistryConfig): NodeRegistryStorage {
    val databaseUrl = config.databaseUrl
    if (databaseUrl == null) {
        return NodeRegistryStore(
            supportedProtocolVersions = config.supportedProtocolVersions,
            heartbeatGraceMilliseconds = config.heartbeatGraceMilliseconds,
            replayRetentionMilliseconds = config.replayRetentionMilliseconds
        )
    }

    val database =
        PostgresNodeRegistryDatabase(
            PostgresNodeRegistryDatabaseConfig(
                jdbcUrl = databaseUrl,
                username = config.databaseUser,
                password = config.databasePassword,
                maximumPoolSize = config.databaseMaximumPoolSize
            )
        )
    return PostgresNodeRegistryStore(
        database = database,
        supportedProtocolVersions = config.supportedProtocolVersions,
        heartbeatGraceMilliseconds = config.heartbeatGraceMilliseconds,
        replayRetentionMilliseconds = config.replayRetentionMilliseconds
    )
}
