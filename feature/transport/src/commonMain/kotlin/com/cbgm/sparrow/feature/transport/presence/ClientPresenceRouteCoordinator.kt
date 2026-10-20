package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayNodeInformation
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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

internal class ClientPresenceRouteCoordinator(
    private val httpClient: HttpClient,
    private val registrationFactory: ClientRouteRegistrationFactory,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider
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
        prefetchedGatewayInformation: Deferred<Result<GatewayInformationSnapshot>>? = null
    ): ClientPresenceRouteSession =
        ClientPresenceRouteSession(
            scope = scope,
            connection = connection,
            prefetchedGatewayInformation = prefetchedGatewayInformation,
            httpClient = httpClient,
            registrationFactory = registrationFactory,
            localBootstrapRoutingIdProvider = localBootstrapRoutingIdProvider,
            localPhoneNumberProvider = localPhoneNumberProvider,
            publishRoute = publishRoute,
            onReady = onReady,
            onFailure = onFailure
        )
}

internal class ClientPresenceRouteSession(
    private val scope: CoroutineScope,
    private val connection: PresenceRouteConnection,
    private var prefetchedGatewayInformation: Deferred<Result<GatewayInformationSnapshot>>?,
    private val httpClient: HttpClient,
    private val registrationFactory: ClientRouteRegistrationFactory,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val publishRoute: suspend (ClientRouteRegistration) -> Result<Unit>,
    private val onReady: (Set<String>) -> Unit,
    private val onFailure: suspend (Throwable) -> Unit
) {
    private val logger = SparrowLog.withTag("ClientPresenceRouteSession")
    private val events = Channel<ClientPresenceRouteEvent>(capacity = Channel.UNLIMITED)
    private var state: ClientPresenceRouteState =
        ClientPresenceRouteState.AwaitingGatewayRegistration
    private var refreshJob: Job? = null
    private var gatewayTimeObservedAt: TimeMark? = null
    private var expirationRetries = 0
    private var recoveringExpiredRoute = false
    private var sentPhoneVerificationToken = false

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
                clearSentPhoneVerificationToken()
                logger.info {
                    "Presence route ready for ${connection.routingId}; aliases=${effect.aliases.size}"
                }
                onReady(effect.aliases)
            }

            is ClientPresenceRouteEffect.Fail -> {
                // A rejected token is not sent again; the reconnect registers without it.
                clearSentPhoneVerificationToken()
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

    // The phone number verified at sign-up is proven once, with the first accepted route.
    private suspend fun loadPhoneVerificationToken(): String? =
        localPhoneNumberProvider
            .getPhoneVerificationToken()
            .onFailure { failure -> logger.warn(failure) { "Could not load the phone token" } }
            .getOrNull()

    private suspend fun clearSentPhoneVerificationToken() {
        if (!sentPhoneVerificationToken) return
        sentPhoneVerificationToken = false
        localPhoneNumberProvider
            .clearPhoneVerificationToken()
            .onFailure { failure -> logger.warn(failure) { "Could not clear the phone token" } }
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
        val aliases =
            localBootstrapRoutingIdProvider
                .getLocalBootstrapRoutingId()
                .onFailure { failure -> SparrowLog.error("ClientPresenceRouteCoordinator", "Could not load bootstrap routing ID", failure) }
                .getOrNull()?.let(::listOf)
                .orEmpty()

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
            ),
            aliases = aliases
        ).fold(
            onSuccess = { registration ->
                val token = if (aliases.isEmpty()) null else loadPhoneVerificationToken()
                sentPhoneVerificationToken = token != null
                events.send(
                    ClientPresenceRouteEvent.RegistrationPrepared(
                        registration = registration.copy(phoneVerificationToken = token)
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
