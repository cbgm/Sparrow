package com.cbgm.sparrow.feature.chats.domain.model.direct

object DirectPendingAuthorizationMessagePolicy {
    const val RETENTION_MILLISECONDS: Long = 2L * 24L * 60L * 60L * 1_000L

    fun expiresAtEpochMilliseconds(createdAtEpochMilliseconds: Long): Long =
        createdAtEpochMilliseconds + RETENTION_MILLISECONDS

    fun isExpired(
        createdAtEpochMilliseconds: Long,
        nowEpochMilliseconds: Long
    ): Boolean =
        nowEpochMilliseconds >= expiresAtEpochMilliseconds(createdAtEpochMilliseconds)
}
