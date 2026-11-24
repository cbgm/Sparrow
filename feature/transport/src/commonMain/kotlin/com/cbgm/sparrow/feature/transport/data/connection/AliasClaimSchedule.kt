package com.cbgm.sparrow.feature.transport.data.connection

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.data.controlplane.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.data.routing.RoutingIdLocalDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

/**
 * Phone aliases go to the Control Planes through the relays. They are claimed at start and again every
 * [ALIAS_CLAIM_INTERVAL_MILLISECONDS], which renews their binding and the invitation mailbox; a claim no Control
 * Plane answered is repeated after [ALIAS_CLAIM_RETRY_MILLISECONDS].
 */
internal class AliasClaimSchedule(
    private val aliasClaims: AliasClaimRegistrar,
    private val routingIdDataSource: RoutingIdLocalDataSource
) {
    private val logger = SparrowLog.withTag("AliasClaimSchedule")

    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            val bound =
                runCatching { aliasClaims.claim(routingIdDataSource.getLocalRoutingId().getOrThrow()) }
                    .onFailure { failure ->
                        if (failure is CancellationException) throw failure
                        logger.warn { "Phone alias claim failed: ${failure::class.simpleName}" }
                    }.getOrDefault(emptySet())
            delay((if (bound.isEmpty()) ALIAS_CLAIM_RETRY_MILLISECONDS else ALIAS_CLAIM_INTERVAL_MILLISECONDS).milliseconds)
        }
    }

    private companion object {
        const val ALIAS_CLAIM_INTERVAL_MILLISECONDS = 6L * 60L * 60L * 1_000L
        const val ALIAS_CLAIM_RETRY_MILLISECONDS = 60_000L
    }
}
