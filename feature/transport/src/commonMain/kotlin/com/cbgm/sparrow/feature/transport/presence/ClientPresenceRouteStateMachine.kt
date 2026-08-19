package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayNodeInformation

internal sealed interface ClientPresenceRouteState {
    data object AwaitingGatewayRegistration : ClientPresenceRouteState

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
    data class GatewayRegistered(
        val gatewayInformation: GatewayNodeInformation
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
    data class PrepareRegistration(
        val gatewayInformation: GatewayNodeInformation
    ) : ClientPresenceRouteEffect

    data class PublishRoute(
        val registration: ClientRouteRegistration
    ) : ClientPresenceRouteEffect

    data class ScheduleRefresh(
        val delayMilliseconds: Long
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
 * Single source of truth for the client bootstrap/presence route lifecycle.
 *
 * Startup is entirely event driven. Gateway registration immediately prepares and publishes the
 * route. The only timer is the protocol-required refresh after the gateway has accepted the route.
 */
internal object ClientPresenceRouteStateMachine {
    fun transition(
        current: ClientPresenceRouteState,
        event: ClientPresenceRouteEvent
    ): ClientPresenceRouteTransition =
        when (current) {
            ClientPresenceRouteState.AwaitingGatewayRegistration -> transitionFromAwaiting(event)
            is ClientPresenceRouteState.PreparingRegistration -> transitionFromPreparing(current, event)
            is ClientPresenceRouteState.PublishingRoute -> transitionFromPublishing(current, event)
            is ClientPresenceRouteState.Ready -> transitionFromReady(current, event)
            is ClientPresenceRouteState.Failed -> ClientPresenceRouteTransition(current)
        }

    private fun transitionFromAwaiting(
        event: ClientPresenceRouteEvent
    ): ClientPresenceRouteTransition {
        val registered = event as? ClientPresenceRouteEvent.GatewayRegistered
            ?: invalidTransition(ClientPresenceRouteState.AwaitingGatewayRegistration, event)

        return preparingRegistration(registered.gatewayInformation)
    }

    private fun transitionFromPreparing(
        current: ClientPresenceRouteState.PreparingRegistration,
        event: ClientPresenceRouteEvent
    ): ClientPresenceRouteTransition =
        when (event) {
            is ClientPresenceRouteEvent.RegistrationPrepared ->
                ClientPresenceRouteTransition(
                    state =
                        ClientPresenceRouteState.PublishingRoute(
                            gatewayInformation = current.gatewayInformation,
                            registration = event.registration
                        ),
                    effects = listOf(ClientPresenceRouteEffect.PublishRoute(event.registration))
                )

            is ClientPresenceRouteEvent.RegistrationPreparationFailed -> failed(event.error)
            else -> invalidTransition(current, event)
        }

    private fun transitionFromPublishing(
        current: ClientPresenceRouteState.PublishingRoute,
        event: ClientPresenceRouteEvent
    ): ClientPresenceRouteTransition =
        when (event) {
            is ClientPresenceRouteEvent.RouteAccepted ->
                ClientPresenceRouteTransition(
                    state =
                        ClientPresenceRouteState.Ready(
                            gatewayInformation = current.gatewayInformation,
                            aliases = event.aliases
                        ),
                    effects =
                        listOf(
                            ClientPresenceRouteEffect.ScheduleRefresh(
                                delayMilliseconds =
                                    current.gatewayInformation.routeRefreshIntervalMilliseconds
                            )
                        )
                )

            is ClientPresenceRouteEvent.RoutePublicationFailed -> failed(event.error)
            is ClientPresenceRouteEvent.RouteRejected -> failed(event.error)
            else -> invalidTransition(current, event)
        }

    private fun transitionFromReady(
        current: ClientPresenceRouteState.Ready,
        event: ClientPresenceRouteEvent
    ): ClientPresenceRouteTransition =
        when (event) {
            ClientPresenceRouteEvent.RefreshDue -> preparingRegistration(current.gatewayInformation)
            is ClientPresenceRouteEvent.RouteRejected -> failed(event.error)
            else -> invalidTransition(current, event)
        }

    private fun preparingRegistration(
        gatewayInformation: GatewayNodeInformation
    ): ClientPresenceRouteTransition =
        ClientPresenceRouteTransition(
            state = ClientPresenceRouteState.PreparingRegistration(gatewayInformation),
            effects = listOf(ClientPresenceRouteEffect.PrepareRegistration(gatewayInformation))
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
