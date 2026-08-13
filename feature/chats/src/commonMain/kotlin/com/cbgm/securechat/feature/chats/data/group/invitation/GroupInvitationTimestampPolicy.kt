package com.cbgm.securechat.feature.chats.data.group.invitation

internal fun resolveInvitationUpdatedAt(
    createdAtEpochMilliseconds: Long,
    candidateAtEpochMilliseconds: Long
): Long =
    maxOf(
        createdAtEpochMilliseconds,
        candidateAtEpochMilliseconds
    )
