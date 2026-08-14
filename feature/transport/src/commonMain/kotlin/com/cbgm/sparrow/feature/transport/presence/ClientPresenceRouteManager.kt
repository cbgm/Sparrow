package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayNodeInformation
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_COMPATIBILITY_CLOCK_SKEW_MILLISECONDS = 30_000L

internal class ClientPresenceRouteManager(
    private val httpClient: HttpClient,
    private val registrationFactory: ClientRouteRegistrationFactory,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider
) {
    private val logger = SparrowLog.withTag("ClientPresenceRouteManager")

    suspend fun fetchGatewayInformation(serverUrl: String): Result<GatewayNodeInformation> =
        runCatching {
            val response = httpClient.get(gatewayInformationUrl(serverUrl))
            val gatewayInformation = response.body<GatewayNodeInformation>()
            val serverTimeEpochMilliseconds =
                response.headers[SERVER_TIME_HEADER]
                    ?.toLongOrNull()

            gatewayInformation.copy(
                serverTimeEpochMilliseconds = serverTimeEpochMilliseconds,
                serverTimeObservedAtEpochMilliseconds =
                    serverTimeEpochMilliseconds?.let { SystemClock.nowEpochMilliseconds() }
            )
        }

    suspend fun createRegistration(
        connection: PresenceRouteConnection,
        gatewayInformation: GatewayNodeInformation
    ): Result<ClientRouteRegistration> {
        val expiresAtEpochMilliseconds =
            routeExpirationEpochMilliseconds(gatewayInformation)
        logger.debug {
            "Preparing signed presence route for ${gatewayInformation.nodeId}; " +
                "expiresAt=$expiresAtEpochMilliseconds; " +
                "gatewayClock=${gatewayInformation.serverTimeEpochMilliseconds != null}"
        }

        return registrationFactory.create(
            routingId = connection.routingId,
            nodeId = gatewayInformation.nodeId,
            connectionId = connection.connectionId,
            generation = connection.generation,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            aliases = bootstrapRoutingAliases()
        )
    }

    private suspend fun bootstrapRoutingAliases(): List<String> =
        localBootstrapRoutingIdProvider
            .getLocalBootstrapRoutingId()
            .getOrNull()
            ?.let(::listOf)
            .orEmpty()

    suspend fun maintain(
        connection: PresenceRouteConnection,
        sendRefresh: suspend (ClientRouteRegistration) -> Result<Unit>,
        reconnect: suspend () -> Unit,
        fail: suspend (Throwable?) -> Unit
    ) {
        var gatewayInformation = connection.initialGatewayInformation
        var routeEstablished = connection.initialRouteEstablished

        while (true) {
            val delayMilliseconds =
                if (routeEstablished) {
                    checkNotNull(gatewayInformation).routeRefreshIntervalMilliseconds
                } else {
                    ROUTE_INFORMATION_RETRY_MILLISECONDS
                }
            delay(delayMilliseconds.milliseconds)

            if (gatewayInformation == null) {
                gatewayInformation =
                    fetchGatewayInformation(serverUrl = connection.serverUrl)
                        .onFailure { error ->
                            logger.warn {
                                "Presence route retry deferred: " +
                                    (error.message ?: "gateway information unavailable")
                            }
                        }.getOrNull()
            }

            val information = gatewayInformation ?: continue
            if (!connection.connectionIdRegistered) {
                logger.info {
                    "Gateway now supports signed presence; reconnecting with a client connection ID"
                }
                reconnect()
                return
            }

            val registration =
                createRegistration(
                    connection = connection,
                    gatewayInformation = information
                ).onFailure { error ->
                    logger.warn {
                        "Presence route signing failed: " +
                            (error.message ?: "unknown error")
                    }
                }.getOrNull() ?: continue

            val refreshResult = sendRefresh(registration)
            if (refreshResult.isSuccess) {
                routeEstablished = true
                logger.debug { "Signed presence route refreshed for ${connection.routingId}" }
            } else if (routeEstablished) {
                fail(refreshResult.exceptionOrNull())
                return
            }
        }
    }

    private companion object {
        const val ROUTE_INFORMATION_RETRY_MILLISECONDS = 5_000L
        const val SERVER_TIME_HEADER = "X-Sparrow-Server-Time"
    }
}

internal data class PresenceRouteConnection(
    val serverUrl: String,
    val routingId: String,
    val connectionId: String,
    val generation: Long,
    val initialGatewayInformation: GatewayNodeInformation?,
    val initialRouteEstablished: Boolean,
    val connectionIdRegistered: Boolean
)

internal fun gatewayInformationUrl(serverUrl: String): String {
    val httpScheme =
        when {
            serverUrl.startsWith("wss://") -> "https://"
            serverUrl.startsWith("ws://") -> "http://"
            else -> error("Gateway WebSocket URL must use ws:// or wss://")
        }
    val authority =
        serverUrl
            .substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .takeIf(String::isNotBlank)
            ?: error("Gateway WebSocket URL must include a host")

    return "$httpScheme$authority/v1/gateway/info"
}

internal fun routeExpirationEpochMilliseconds(
    gatewayInformation: GatewayNodeInformation,
    localNowEpochMilliseconds: Long = SystemClock.nowEpochMilliseconds()
): Long {
    val serverTimeEpochMilliseconds = gatewayInformation.serverTimeEpochMilliseconds
    if (serverTimeEpochMilliseconds != null) {
        val elapsedSinceObservation =
            gatewayInformation.serverTimeObservedAtEpochMilliseconds
                ?.let { observedAt ->
                    (localNowEpochMilliseconds - observedAt).coerceAtLeast(0L)
                }
                ?: 0L
        return serverTimeEpochMilliseconds +
            elapsedSinceObservation +
            gatewayInformation.routeLifetimeMilliseconds
    }

    val maximumSafetyMargin =
        (
            gatewayInformation.routeLifetimeMilliseconds -
                gatewayInformation.routeRefreshIntervalMilliseconds -
                1L
        ).coerceAtLeast(0L)
    val preferredSafetyMargin =
        minOf(
            MAX_COMPATIBILITY_CLOCK_SKEW_MILLISECONDS,
            gatewayInformation.routeLifetimeMilliseconds / 3L
        )
    val safetyMargin = minOf(preferredSafetyMargin, maximumSafetyMargin)

    return localNowEpochMilliseconds +
        gatewayInformation.routeLifetimeMilliseconds -
        safetyMargin
}
