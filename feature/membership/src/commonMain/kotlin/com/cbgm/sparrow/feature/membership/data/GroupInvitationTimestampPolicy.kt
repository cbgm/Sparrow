package com.cbgm.sparrow.feature.membership.data

fun resolveInvitationUpdatedAt(
    createdAtEpochMilliseconds: Long,
    candidateAtEpochMilliseconds: Long
): Long =
    maxOf(
        createdAtEpochMilliseconds,
        candidateAtEpochMilliseconds
    )
