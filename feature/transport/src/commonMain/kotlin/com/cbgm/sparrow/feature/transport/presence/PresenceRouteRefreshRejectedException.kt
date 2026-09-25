package com.cbgm.sparrow.feature.transport.presence

/** Expected gateway presence housekeeping rejection; reconnect/retry without UI error. */
internal class PresenceRouteRefreshRejectedException(
    code: String,
    val isExpiration: Boolean
) : IllegalStateException("Presence route rejected by gateway: $code")
