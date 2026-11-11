package com.cbgm.sparrow.feature.transport.presence

/** Whether a Control Plane bound the device's phone alias to it ([AliasClaimRegistrar] claims it). */
internal fun interface PhoneAliasBinding {
    /**
     * Waits until a Control Plane bound [alias] to this device, at most [timeoutMilliseconds]
     * (a [kotlinx.coroutines.TimeoutCancellationException] after that). A first invitation names the phone alias as
     * its sender, and recipients check it at their Control Planes.
     */
    suspend fun awaitBound(
        alias: String,
        timeoutMilliseconds: Long
    )
}
