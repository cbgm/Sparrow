package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayNodeInformation
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_COMPATIBILITY_CLOCK_SKEW_MILLISECONDS = 30_000L
private const val SERVER_TIME_HEADER = "X-Sparrow-Server-Time"

internal class ClientPresenceRouteCoordinator(
    private val httpClient: HttpClient,
    private val registrationFactory: ClientRouteRegistrationFactory,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider
) {
    fun createSession(
        scope: CoroutineScope,
        connection: PresenceRouteConnection,
        publishRoute: suspend (ClientRouteRegistration) -> Result<Unit>,
        onReady: (Set<String>) -> Unit,
        onFailure: suspend (Throwable) -> Unit
    ): ClientPresenceRouteSession =
        ClientPresenceRouteSession(
            scope = scope,
            connection = connection,
            httpClient = httpClient,
            registrationFactory = registrationFactory,
            localBootstrapRoutingIdProvider = localBootstrapRoutingIdProvider,
            publishRoute = publishRoute,
            onReady = onReady,
            onFailure = onFailure
        )
}

internal class ClientPresenceRouteSession(
    private val scope: CoroutineScope,
    private val connection: PresenceRouteConnection,
    private val httpClient: HttpClient,
    private val registrationFactory: ClientRouteRegistrationFactory,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider,
    private val publishRoute: suspend (ClientRouteRegistration) -> Result<Unit>,
    private val onReady: (Set<String>) -> Unit,
    private val onFailure: suspend (Throwable) -> Unit
) {
    private val logger = SparrowLog.withTag("ClientPresenceRouteSession")
    private val events = Channel<ClientPresenceRouteEvent>(capacity = Channel.UNLIMITED)
    private var state: ClientPresenceRouteState =
        ClientPresenceRouteState.AwaitingGatewayRegistration
    private var refreshJob: Job? = null

    private val runner =
        scope.launch {
            for (event in events) {
                val transition = ClientPresenceRouteStateMachine.transition(state, event)
                state = transition.state
                transition.effects.forEach { effect -> execute(effect) }
            }
        }

    fun onGatewayRegistered() {
        events.trySend(ClientPresenceRouteEvent.GatewayRegistered)
    }

    fun onRouteAccepted(aliases: Set<String>) {
        events.trySend(ClientPresenceRouteEvent.RouteAccepted(aliases = aliases))
    }

    fun onRouteRejected(error: Throwable) {
        events.trySend(ClientPresenceRouteEvent.RouteRejected(error = error))
    }

    fun close() {
        refreshJob?.cancel()
        events.close()
        runner.cancel()
    }

    private suspend fun execute(effect: ClientPresenceRouteEffect) {
        when (effect) {
            ClientPresenceRouteEffect.LoadGatewayInformation -> loadGatewayInformation()

            is ClientPresenceRouteEffect.PrepareRegistration ->
                prepareRegistration(effect.gatewayInformation)

            is ClientPresenceRouteEffect.PublishRoute ->
                publishRegistration(effect.registration)

            is ClientPresenceRouteEffect.ScheduleRefresh ->
                scheduleRefresh(effect.delayMilliseconds)

            is ClientPresenceRouteEffect.AnnounceReady -> {
                logger.info {
                    "Presence route ready for ${connection.routingId}; aliases=${effect.aliases.size}"
                }
                onReady(effect.aliases)
            }

            is ClientPresenceRouteEffect.Fail -> {
                logger.warn { "Presence route failed: ${effect.error.message ?: "unknown error"}" }
                onFailure(effect.error)
            }
        }
    }

    private suspend fun loadGatewayInformation() {
        runCatching {
            val response = httpClient.get(gatewayInformationUrl(connection.serverUrl))
            val gatewayInformation = response.body<GatewayNodeInformation>()
            val serverTimeEpochMilliseconds =
                response.headers[SERVER_TIME_HEADER]
                    ?.toLongOrNull()

            gatewayInformation.copy(
                serverTimeEpochMilliseconds = serverTimeEpochMilliseconds,
                serverTimeObservedAtEpochMilliseconds =
                    serverTimeEpochMilliseconds?.let { SystemClock.nowEpochMilliseconds() }
            )
        }.fold(
            onSuccess = { gatewayInformation ->
                events.send(
                    ClientPresenceRouteEvent.GatewayInformationLoaded(
                        gatewayInformation = gatewayInformation
                    )
                )
            },
            onFailure = { error ->
                events.send(
                    ClientPresenceRouteEvent.GatewayInformationLoadFailed(
                        error = error
                    )
                )
            }
        )
    }

    private suspend fun prepareRegistration(gatewayInformation: GatewayNodeInformation) {
        val aliases =
            localBootstrapRoutingIdProvider
                .getLocalBootstrapRoutingId()
                .getOrNull()
                ?.let(::listOf)
                .orEmpty()

        registrationFactory.create(
            routingId = connection.routingId,
            nodeId = gatewayInformation.nodeId,
            connectionId = connection.connectionId,
            generation = connection.generation,
            expiresAtEpochMilliseconds = routeExpirationEpochMilliseconds(gatewayInformation),
            aliases = aliases
        ).fold(
            onSuccess = { registration ->
                events.send(
                    ClientPresenceRouteEvent.RegistrationPrepared(
                        registration = registration
                    )
                )
            },
            onFailure = { error ->
                events.send(
                    ClientPresenceRouteEvent.RegistrationPreparationFailed(
                        error = error
                    )
                )
            }
        )
    }

    private suspend fun publishRegistration(registration: ClientRouteRegistration) {
        publishRoute(registration).onFailure { error ->
            events.send(
                ClientPresenceRouteEvent.RoutePublicationFailed(
                    error = error
                )
            )
        }
    }

    private fun scheduleRefresh(delayMilliseconds: Long) {
        refreshJob?.cancel()
        refreshJob =
            scope.launch {
                delay(delayMilliseconds.milliseconds)
                events.send(ClientPresenceRouteEvent.RefreshDue)
            }
    }
}

internal data class PresenceRouteConnection(
    val serverUrl: String,
    val routingId: String,
    val connectionId: String,
    val generation: Long
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
