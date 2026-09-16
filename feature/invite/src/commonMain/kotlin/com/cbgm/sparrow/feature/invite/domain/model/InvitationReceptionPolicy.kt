package com.cbgm.sparrow.feature.invite.domain.model

data class InvitationReceptionPolicy(
    val enabled: Boolean,
    val blockedPeerIds: Set<String>,
    val blockUnknownPeers: Boolean
)
