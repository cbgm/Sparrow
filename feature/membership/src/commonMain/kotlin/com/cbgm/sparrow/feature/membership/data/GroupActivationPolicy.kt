package com.cbgm.sparrow.feature.membership.data

fun canSendToActiveGroupMembers(activeParticipantCount: Int): Boolean = activeParticipantCount > 0
