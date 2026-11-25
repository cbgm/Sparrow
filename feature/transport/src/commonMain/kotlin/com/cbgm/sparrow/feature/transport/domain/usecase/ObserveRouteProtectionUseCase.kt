package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.model.RouteProtection
import com.cbgm.sparrow.feature.transport.domain.repository.RouteProtectionRepository
import kotlinx.coroutines.flow.StateFlow

/** How well the route hides the device; anything less than protected shows only once it lasted a while. */
class ObserveRouteProtectionUseCase(
    private val source: RouteProtectionRepository
) {
    operator fun invoke(): StateFlow<RouteProtection> = source.protection
}
