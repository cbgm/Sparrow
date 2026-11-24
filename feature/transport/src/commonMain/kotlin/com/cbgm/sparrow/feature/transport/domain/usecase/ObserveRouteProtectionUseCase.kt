package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.circuit.CircuitFallbackStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.circuit.CircuitStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlin.time.Duration.Companion.milliseconds

/** How well the route to the mailboxes protects the user right now. */
enum class RouteProtection {
    /** Every circuit is up through the chosen hops, or the first ones are still being built. */
    PROTECTED,

    /** Fewer hops than chosen, or no circuit while messages wait for one. */
    REDUCED,

    /** No circuit, and requests go out directly: the mailbox node sees the device's address. */
    DIRECT
}

/**
 * The [RouteProtection], where anything less than protected counts only once it lasted [SETTLE_MILLISECONDS]: a
 * circuit that is rebuilt for a moment changes nothing on screen. One state for the whole app, so the shield in the
 * chats and Onion routing always agree, however late a screen starts watching; a circuit being built keeps the last
 * state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveRouteProtectionUseCase(
    circuitRunner: CircuitRunner,
    fallbackStore: CircuitFallbackStore,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val protection: StateFlow<RouteProtection> =
        combine(circuitRunner.status, fallbackStore.observeSendsDirectly(), ::protectionOf)
            .filterNotNull()
            .distinctUntilChanged()
            .transformLatest { protection ->
                if (protection != RouteProtection.PROTECTED) delay(SETTLE_MILLISECONDS.milliseconds)
                emit(protection)
            }.stateIn(scope, SharingStarted.Eagerly, RouteProtection.PROTECTED)

    operator fun invoke(): StateFlow<RouteProtection> = protection

    /** Null while a circuit is being built: that alone changes nothing. */
    private fun protectionOf(
        status: CircuitStatus,
        sendsDirectly: Boolean
    ): RouteProtection? =
        when {
            status is CircuitStatus.Building -> null
            status is CircuitStatus.Connected && status.hopCount < status.requestedHopCount -> RouteProtection.REDUCED
            status is CircuitStatus.Unavailable && sendsDirectly -> RouteProtection.DIRECT
            status is CircuitStatus.Unavailable -> RouteProtection.REDUCED
            else -> RouteProtection.PROTECTED
        }

    private companion object {
        const val SETTLE_MILLISECONDS = 15_000L
    }
}
