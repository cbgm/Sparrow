package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

/**
 * Group title, description, avatar and pin are last-writer-wins states versioned by their change time.
 * A local change always moves the version forward, even when the clock is behind the stored version.
 */
object GroupStateVersionPolicy {
    fun nextChangedAt(previousChangedAtEpochMilliseconds: Long, nowEpochMilliseconds: Long): Long =
        maxOf(nowEpochMilliseconds, previousChangedAtEpochMilliseconds + 1L)

    /** Only a strictly newer version replaces the stored one, so replays and equal versions are ignored. */
    fun isNewer(incomingChangedAtEpochMilliseconds: Long, currentChangedAtEpochMilliseconds: Long): Boolean =
        incomingChangedAtEpochMilliseconds > currentChangedAtEpochMilliseconds
}
