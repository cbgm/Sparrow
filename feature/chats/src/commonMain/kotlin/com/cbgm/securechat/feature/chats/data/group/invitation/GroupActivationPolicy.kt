package com.cbgm.securechat.feature.chats.data.group.invitation

internal fun canSendToActiveGroupMembers(activeParticipantCount: Int): Boolean = activeParticipantCount > 0
