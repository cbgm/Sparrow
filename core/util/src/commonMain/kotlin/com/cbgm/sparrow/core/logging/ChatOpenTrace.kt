package com.cbgm.sparrow.core.logging

import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** TEMPORARY diagnostic only. Remove after collecting chat-opening timings.
 * Uses monotonic time; records no conversation, contact or message identifiers.
 * Events are enabled only for a short interval following a conversation tap.
 */
object ChatOpenTrace {
    private val logger = SparrowLog.withTag("ChatOpenTrace")
    private var startedAt: TimeMark? = null
    private var currentKind: String? = null

    fun begin(kind: String) {
        currentKind = kind
        startedAt = TimeSource.Monotonic.markNow()
        logger.info { "[$kind +0ms] overview tap -> navigation requested" }
    }

    fun event(stage: String) {
        val start = startedAt ?: return
        val elapsed = start.elapsedNow().inWholeMilliseconds
        if (elapsed > 6_000L) return
        logger.info { "[${currentKind ?: "chat"} +${elapsed}ms] $stage" }
    }
}
