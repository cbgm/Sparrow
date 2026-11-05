package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayNodeInformation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val MAX_COMPATIBILITY_CLOCK_SKEW_MILLISECONDS = 30_000L
private const val MAX_ROUTE_TTL_SAFETY_MARGIN_MILLISECONDS = 5_000L
private const val MAX_EXPIRATION_RETRIES = 3
private const val SERVER_TIME_HEADER = "X-Sparrow-Server-Time"
private const val ALIAS_CLAIM_INTERVAL_MILLISECONDS = 6L * 60L * 60L * 1_000L

internal class ClientPresenceRouteCoordinator(
    private val httpClient: HttpClient,
    private val registrationFactory: ClientRouteRegistrationFactory,
    private val aliasClaimRegistrar: AliasClaimRegistrar
) {
    // Starts the public gateway information request while the WebSocket is still opening, so
    // both cold HTTPS connections overlap instead of running one after the other.
    fun prefetchGatewayInformation(
        scope: CoroutineScope,
        serverUrl: String
    ): Deferred<Result<GatewayInformationSnapshot>> =
        scope.async { runCatching { httpClient.fetchGatewayInformation(serverUrl) } }

    fun createSession(
        scope: CoroutineScope,
        connection: PresenceRouteConnection,
        publishRoute: suspend (ClientRouteRegistration) -> Result<Unit>,
        onReady: (Set<String>) -> Unit,
        onFailure: suspend (Throwable) -> Unit,
        prefetchedGatewayInformation: Deferred<Result<GatewayInformationSnapshot>>? = null,
        onAliasesClaimed: (Set<String>) -> Unit = {}
    ): ClientPresenceRouteSession =
        ClientPresenceRouteSession(
            scope = scope,
            connection = connection,
            prefetchedGatewayInformation = prefetchedGatewayInformation,
            httpClient = httpClient,
            registrationFactory = registrationFactory,
            aliasClaimRegistrar = aliasClaimRegistrar,
            publishRoute = publishRoute,
            onReady = onReady,
            onFailure = onFailure,
            onAliasesClaimed = onAliasesClaimed
        )
}

internal class ClientPresenceRouteSession(
    private val scope: CoroutineScope,
    private val connection: PresenceRouteConnection,
    private var prefetchedGatewayInformation: Deferred<Result<GatewayInformationSnapshot>>?,
    private val httpClient: HttpClient,
    private val registrationFactory: ClientRouteRegistrationFactory,
    private val aliasClaimRegistrar: AliasClaimRegistrar,
    private val publishRoute: suspend (ClientRouteRegistration) -> Result<Unit>,
    private val onReady: (Set<String>) -> Unit,
    private val onFailure: suspend (Throwable) -> Unit,
    private val onAliasesClaimed: (Set<String>) -> Unit
) {
    private val logger = SparrowLog.withTag("ClientPresenceRouteSession")
    private val events = Channel<ClientPresenceRouteEvent>(capacity = Channel.UNLIMITED)
    private var state: ClientPresenceRouteState =
        ClientPresenceRouteState.AwaitingGatewayRegistration
    private var refreshJob: Job? = null
    private var gatewayTimeObservedAt: TimeMark? = null
    private var expirationRetries = 0
    private var recoveringExpiredRoute = false
    private var aliasClaimJob: Job? = null

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

    fun onRouteAccepted() {
        events.trySend(ClientPresenceRouteEvent.RouteAccepted(aliases = emptySet()))
    }

    fun onRouteRejected(error: Throwable) {
        if (error is PresenceRouteRefreshRejectedException && error.isExpiration &&
            expirationRetries < MAX_EXPIRATION_RETRIES
        ) {
            expirationRetries++
            recoveringExpiredRoute = true
            refreshJob?.cancel()
            logger.debug {
                "Presence refresh expired; obtaining fresh gateway time " +
                    "(retry $expirationRetries/$MAX_EXPIRATION_RETRIES)"
            }
            events.trySend(ClientPresenceRouteEvent.RouteExpired)
        } else {
            // A sustained route rejection is handled by normal transport reconnect;
            // this is not a foreground messaging failure or a global snackbar.
            events.trySend(ClientPresenceRouteEvent.RouteRejected(error = error))
        }
    }

    fun close() {
        refreshJob?.cancel()
        aliasClaimJob?.cancel()
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
                expirationRetries = 0
                recoveringExpiredRoute = false
                logger.info { "Presence route ready for ${connection.routingId}" }
                onReady(effect.aliases)
                startAliasClaims()
            }

            is ClientPresenceRouteEffect.Fail -> {
                if (recoveringExpiredRoute || effect.error is PresenceRouteRefreshRejectedException) {
                    // Expected transport housekeeping failure; reconnect will retry.
                    // Error logging here would display an intrusive global snackbar.
                    logger.warn { "Presence route refresh rejected; reconnecting" }
                } else {
                    logger.error(effect.error) { "Presence route failed" }
                }
                onFailure(effect.error)
            }
        }
    }

    /**
     * Phone aliases go to the Control Planes through the relays, not through the gateway. They are claimed once the
     * route is ready and again every [ALIAS_CLAIM_INTERVAL_MILLISECONDS], which renews their binding.
     */
    private fun startAliasClaims() {
        if (aliasClaimJob?.isActive == true) return
        aliasClaimJob =
            scope.launch {
                while (true) {
                    val aliases =
                        runCatching { aliasClaimRegistrar.claim(connection.routingId) }
                            .onFailure { failure ->
                                if (failure is CancellationException) throw failure
                                logger.warn { "Phone alias claim failed: ${failure::class.simpleName}" }
                            }.getOrDefault(emptySet())
                    onAliasesClaimed(aliases)
                    delay(ALIAS_CLAIM_INTERVAL_MILLISECONDS.milliseconds)
                }
            }
    }

    // The first load uses the prefetched response; refreshes and expiration recovery always
    // fetch again because they need a current server time.
    private suspend fun loadGatewayInformation() {
        val prefetched = prefetchedGatewayInformation?.also { prefetchedGatewayInformation = null }
        val result =
            prefetched?.await()?.takeIf(Result<GatewayInformationSnapshot>::isSuccess)
                ?: runCatching { httpClient.fetchGatewayInformation(connection.serverUrl) }
        result.fold(
            onSuccess = { snapshot ->
                gatewayTimeObservedAt = snapshot.observedAt
                events.send(
                    ClientPresenceRouteEvent.GatewayInformationLoaded(
                        gatewayInformation = snapshot.information
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
        registrationFactory.create(
            routingId = connection.routingId,
            nodeId = gatewayInformation.nodeId,
            connectionId = connection.connectionId,
            generation = connection.generation,
            expiresAtEpochMilliseconds = routeExpirationEpochMilliseconds(
                gatewayInformation = gatewayInformation,
                localNowEpochMilliseconds =
                    gatewayInformation.serverTimeObservedAtEpochMilliseconds?.let { observedAt ->
                        observedAt + (gatewayTimeObservedAt?.elapsedNow()?.inWholeMilliseconds ?: 0L)
                    } ?: SystemClock.nowEpochMilliseconds()
            )
        ).fold(
            onSuccess = { registration ->
                events.send(ClientPresenceRouteEvent.RegistrationPrepared(registration = registration))
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
            SparrowLog.error("ClientPresenceRouteCoordinator", "Client route publication failed", error)
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

internal class GatewayInformationSnapshot(
    val information: GatewayNodeInformation,
    val observedAt: TimeMark?
)

internal suspend fun HttpClient.fetchGatewayInformation(serverUrl: String): GatewayInformationSnapshot {
    val response = get(gatewayInformationUrl(serverUrl))
    val gatewayInformation = response.body<GatewayNodeInformation>()
    val serverTimeEpochMilliseconds =
        response.headers[SERVER_TIME_HEADER]
            ?.toLongOrNull()

    // Keep a monotonic observation as well as the original epoch value.
    // Changing the device wall clock while Sparrow is open must not
    // manufacture a route that expires in the past or exceeds the TTL.
    val observedAt = serverTimeEpochMilliseconds?.let { TimeSource.Monotonic.markNow() }
    return GatewayInformationSnapshot(
        information =
            gatewayInformation.copy(
                serverTimeEpochMilliseconds = serverTimeEpochMilliseconds,
                serverTimeObservedAtEpochMilliseconds =
                    serverTimeEpochMilliseconds?.let { SystemClock.nowEpochMilliseconds() }
            ),
        observedAt = observedAt
    )
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
    val maximumSafetyMargin =
        (
            gatewayInformation.routeLifetimeMilliseconds -
                gatewayInformation.routeRefreshIntervalMilliseconds -
                1L
        ).coerceAtLeast(0L)
    val ttlSafetyMargin = minOf(
        MAX_ROUTE_TTL_SAFETY_MARGIN_MILLISECONDS,
        gatewayInformation.routeLifetimeMilliseconds / 6L,
        maximumSafetyMargin
    )

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
            gatewayInformation.routeLifetimeMilliseconds -
            ttlSafetyMargin
    }

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
