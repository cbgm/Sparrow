package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair

/** Membership confirmed the local admin for the current epoch; signed group metadata goes to [recipientContactIds]. */
data class GroupAdminContext(
    val groupId: String,
    val epoch: Int,
    val signingKeyPair: LocalSigningKeyPair,
    val recipientContactIds: Set<String>
)
