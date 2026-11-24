package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.RouteProtectionSource
import com.cbgm.sparrow.feature.transport.domain.model.RouteProtection
import kotlinx.coroutines.flow.StateFlow

/** How well the route hides the device; anything less than protected shows only once it lasted a while. */
class ObserveRouteProtectionUseCase(
    private val source: RouteProtectionSource
) {
    operator fun invoke(): StateFlow<RouteProtection> = source.protection
}
