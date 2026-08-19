package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayNodeInformation
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_COMPATIBILITY_CLOCK_SKEW_MILLISECONDS = 30_000L

internal class ClientPresenceRouteCoordinator(
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

    fun onGatewayRegistered(gatewayInformation: GatewayNodeInformation) {
        events.trySend(
            ClientPresenceRouteEvent.GatewayRegistered(
                gatewayInformation = gatewayInformation
            )
        )
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
            is ClientPresenceRouteEffect.PrepareRegistration ->
                prepareRegistration(effect.gatewayInformation)

            is ClientPresenceRouteEffect.PublishRoute ->
                publishRegistration(effect.registration)

            is ClientPresenceRouteEffect.ScheduleRefresh ->
                scheduleRefresh(effect.delayMilliseconds)

            is ClientPresenceRouteEffect.Fail -> {
                logger.warn { "Presence route failed: ${effect.error.message ?: "unknown error"}" }
                onFailure(effect.error)
            }
        }
    }

    private suspend fun prepareRegistration(gatewayInformation: GatewayNodeInformation) {
        val aliases =
            localBootstrapRoutingIdProvider
                .getLocalBootstrapRoutingId()
                .getOrNull()
                ?.let(::listOf)
                .orEmpty()

        val result =
            registrationFactory.create(
                routingId = connection.routingId,
                nodeId = gatewayInformation.nodeId,
                connectionId = connection.connectionId,
                generation = connection.generation,
                expiresAtEpochMilliseconds = routeExpirationEpochMilliseconds(gatewayInformation),
                aliases = aliases
            )

        result.fold(
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
        publishRoute(registration).fold(
            onSuccess = {
                logger.debug {
                    "Signed presence route sent for ${connection.routingId}; awaiting gateway acceptance"
                }
            },
            onFailure = { error ->
                events.send(
                    ClientPresenceRouteEvent.RoutePublicationFailed(
                        error = error
                    )
                )
            }
        )
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
    val routingId: String,
    val connectionId: String,
    val generation: Long
)

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
