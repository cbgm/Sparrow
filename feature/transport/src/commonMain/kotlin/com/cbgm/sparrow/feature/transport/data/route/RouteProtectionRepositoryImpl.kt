package com.cbgm.sparrow.feature.transport.data.route

import com.cbgm.sparrow.feature.transport.domain.model.CircuitStatus
import com.cbgm.sparrow.feature.transport.domain.model.RouteProtection
import com.cbgm.sparrow.feature.transport.domain.port.CircuitPort
import com.cbgm.sparrow.feature.transport.domain.repository.RouteProtectionRepository
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
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

/**
 * The [RouteProtection], where anything less than protected counts only once it lasted [SETTLE_MILLISECONDS]: a
 * circuit that is rebuilt for a moment changes nothing on screen. One state for the whole app, so the shield in the
 * chats and Onion routing always agree, however late a screen starts watching; a circuit being built keeps the last
 * state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class RouteProtectionRepositoryImpl(
    circuits: CircuitPort,
    preferences: TransportPreferencesRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : RouteProtectionRepository {
    override val protection: StateFlow<RouteProtection> =
        combine(circuits.status, preferences.observeSendsDirectly(), ::protectionOf)
            .filterNotNull()
            .distinctUntilChanged()
            .transformLatest { protection ->
                if (protection != RouteProtection.PROTECTED) delay(SETTLE_MILLISECONDS.milliseconds)
                emit(protection)
            }.stateIn(scope, SharingStarted.Eagerly, RouteProtection.PROTECTED)

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
