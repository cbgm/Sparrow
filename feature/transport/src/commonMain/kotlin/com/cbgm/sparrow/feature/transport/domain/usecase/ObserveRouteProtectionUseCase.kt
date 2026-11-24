package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.circuit.CircuitFallbackStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.circuit.CircuitStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
 * circuit that is rebuilt for a moment changes nothing on screen.
 */
class ObserveRouteProtectionUseCase(
    private val circuitRunner: CircuitRunner,
    private val fallbackStore: CircuitFallbackStore
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<RouteProtection> =
        combine(circuitRunner.status, fallbackStore.observeSendsDirectly(), ::protection)
            .distinctUntilChanged()
            .transformLatest { protection ->
                if (protection != RouteProtection.PROTECTED) delay(SETTLE_MILLISECONDS.milliseconds)
                emit(protection)
            }.distinctUntilChanged()

    private fun protection(
        status: CircuitStatus,
        sendsDirectly: Boolean
    ): RouteProtection =
        when {
            status is CircuitStatus.Connected && status.hopCount < status.requestedHopCount -> RouteProtection.REDUCED
            status is CircuitStatus.Unavailable && sendsDirectly -> RouteProtection.DIRECT
            status is CircuitStatus.Unavailable -> RouteProtection.REDUCED
            else -> RouteProtection.PROTECTED
        }

    private companion object {
        const val SETTLE_MILLISECONDS = 15_000L
    }
}
