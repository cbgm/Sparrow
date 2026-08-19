package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayNodeInformation

internal sealed interface ClientPresenceRouteState {
    data object AwaitingGatewayRegistration : ClientPresenceRouteState

    data object LoadingGatewayInformation : ClientPresenceRouteState

    data class PreparingRegistration(
        val gatewayInformation: GatewayNodeInformation
    ) : ClientPresenceRouteState

    data class PublishingRoute(
        val gatewayInformation: GatewayNodeInformation,
        val registration: ClientRouteRegistration
    ) : ClientPresenceRouteState

    data class Ready(
        val gatewayInformation: GatewayNodeInformation,
        val aliases: Set<String>
    ) : ClientPresenceRouteState

    data class Failed(
        val error: Throwable
    ) : ClientPresenceRouteState
}

internal sealed interface ClientPresenceRouteEvent {
    data object GatewayRegistered : ClientPresenceRouteEvent

    data class GatewayInformationLoaded(
        val gatewayInformation: GatewayNodeInformation
    ) : ClientPresenceRouteEvent

    data class GatewayInformationLoadFailed(
        val error: Throwable
    ) : ClientPresenceRouteEvent

    data class RegistrationPrepared(
        val registration: ClientRouteRegistration
    ) : ClientPresenceRouteEvent

    data class RegistrationPreparationFailed(
        val error: Throwable
    ) : ClientPresenceRouteEvent

    data class RouteAccepted(
        val aliases: Set<String>
    ) : ClientPresenceRouteEvent

    data class RoutePublicationFailed(
        val error: Throwable
    ) : ClientPresenceRouteEvent

    data class RouteRejected(
        val error: Throwable
    ) : ClientPresenceRouteEvent

    data object RefreshDue : ClientPresenceRouteEvent
}

internal sealed interface ClientPresenceRouteEffect {
    data object LoadGatewayInformation : ClientPresenceRouteEffect

    data class PrepareRegistration(
        val gatewayInformation: GatewayNodeInformation
    ) : ClientPresenceRouteEffect

    data class PublishRoute(
        val registration: ClientRouteRegistration
    ) : ClientPresenceRouteEffect

    data class ScheduleRefresh(
        val delayMilliseconds: Long
    ) : ClientPresenceRouteEffect

    data class AnnounceReady(
        val aliases: Set<String>
    ) : ClientPresenceRouteEffect

    data class Fail(
        val error: Throwable
    ) : ClientPresenceRouteEffect
}

internal data class ClientPresenceRouteTransition(
    val state: ClientPresenceRouteState,
    val effects: List<ClientPresenceRouteEffect> = emptyList()
)

/**
 * Single source of truth for bootstrap/presence route initialization and refresh.
 *
 * Startup is entirely reactive:
 * gateway registration -> gateway information -> signed route -> gateway acknowledgement -> ready.
 * The only timer is the protocol refresh timer after the route is ready.
 */
internal object ClientPresenceRouteStateMachine {
    fun transition(
        current: ClientPresenceRouteState,
        event: ClientPresenceRouteEvent
    ): ClientPresenceRouteTransition =
        when {
            current is ClientPresenceRouteState.AwaitingGatewayRegistration &&
                event is ClientPresenceRouteEvent.GatewayRegistered ->
                ClientPresenceRouteTransition(
                    state = ClientPresenceRouteState.LoadingGatewayInformation,
                    effects = listOf(ClientPresenceRouteEffect.LoadGatewayInformation)
                )

            current is ClientPresenceRouteState.LoadingGatewayInformation &&
                event is ClientPresenceRouteEvent.GatewayInformationLoaded ->
                preparingRegistration(event.gatewayInformation)

            current is ClientPresenceRouteState.LoadingGatewayInformation &&
                event is ClientPresenceRouteEvent.GatewayInformationLoadFailed ->
                failed(event.error)

            current is ClientPresenceRouteState.PreparingRegistration &&
                event is ClientPresenceRouteEvent.RegistrationPrepared ->
                ClientPresenceRouteTransition(
                    state =
                        ClientPresenceRouteState.PublishingRoute(
                            gatewayInformation = current.gatewayInformation,
                            registration = event.registration
                        ),
                    effects = listOf(ClientPresenceRouteEffect.PublishRoute(event.registration))
                )

            current is ClientPresenceRouteState.PreparingRegistration &&
                event is ClientPresenceRouteEvent.RegistrationPreparationFailed ->
                failed(event.error)

            current is ClientPresenceRouteState.PublishingRoute &&
                event is ClientPresenceRouteEvent.RouteAccepted ->
                ready(
                    gatewayInformation = current.gatewayInformation,
                    aliases = event.aliases
                )

            current is ClientPresenceRouteState.PublishingRoute &&
                event is ClientPresenceRouteEvent.RoutePublicationFailed ->
                failed(event.error)

            current is ClientPresenceRouteState.PublishingRoute &&
                event is ClientPresenceRouteEvent.RouteRejected ->
                failed(event.error)

            current is ClientPresenceRouteState.Ready &&
                event is ClientPresenceRouteEvent.RefreshDue ->
                preparingRegistration(current.gatewayInformation)

            current is ClientPresenceRouteState.Ready &&
                event is ClientPresenceRouteEvent.RouteRejected ->
                failed(event.error)

            current is ClientPresenceRouteState.Failed ->
                ClientPresenceRouteTransition(current)

            else -> invalidTransition(current, event)
        }

    private fun preparingRegistration(
        gatewayInformation: GatewayNodeInformation
    ): ClientPresenceRouteTransition =
        ClientPresenceRouteTransition(
            state = ClientPresenceRouteState.PreparingRegistration(gatewayInformation),
            effects = listOf(ClientPresenceRouteEffect.PrepareRegistration(gatewayInformation))
        )

    private fun ready(
        gatewayInformation: GatewayNodeInformation,
        aliases: Set<String>
    ): ClientPresenceRouteTransition =
        ClientPresenceRouteTransition(
            state =
                ClientPresenceRouteState.Ready(
                    gatewayInformation = gatewayInformation,
                    aliases = aliases
                ),
            effects =
                listOf(
                    ClientPresenceRouteEffect.AnnounceReady(aliases),
                    ClientPresenceRouteEffect.ScheduleRefresh(
                        delayMilliseconds = gatewayInformation.routeRefreshIntervalMilliseconds
                    )
                )
        )

    private fun failed(error: Throwable): ClientPresenceRouteTransition =
        ClientPresenceRouteTransition(
            state = ClientPresenceRouteState.Failed(error),
            effects = listOf(ClientPresenceRouteEffect.Fail(error))
        )

    private fun invalidTransition(
        current: ClientPresenceRouteState,
        event: ClientPresenceRouteEvent
    ): Nothing =
        error("Invalid client presence route transition: $current + $event")
}
