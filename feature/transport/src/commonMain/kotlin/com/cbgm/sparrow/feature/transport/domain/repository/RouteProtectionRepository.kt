package com.cbgm.sparrow.feature.transport.domain.repository

import com.cbgm.sparrow.feature.transport.domain.model.RouteProtection
import kotlinx.coroutines.flow.StateFlow

/** How well the route hides the device right now, one state for the whole app. */
interface RouteProtectionRepository {
    val protection: StateFlow<RouteProtection>
}
