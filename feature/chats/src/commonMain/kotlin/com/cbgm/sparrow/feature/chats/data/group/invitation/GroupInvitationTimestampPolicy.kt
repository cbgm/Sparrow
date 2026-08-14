package com.cbgm.sparrow.feature.chats.data.group.invitation

internal fun resolveInvitationUpdatedAt(
    createdAtEpochMilliseconds: Long,
    candidateAtEpochMilliseconds: Long
): Long =
    maxOf(
        createdAtEpochMilliseconds,
        candidateAtEpochMilliseconds
    )
