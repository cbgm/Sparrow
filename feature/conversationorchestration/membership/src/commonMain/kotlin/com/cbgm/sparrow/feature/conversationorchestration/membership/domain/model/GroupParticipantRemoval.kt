package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalReason
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult

/** Membership removed [result]'s member; the history event names them [memberDisplayName]. */
data class GroupParticipantRemoval(
    val result: GroupMemberRemovalResult,
    val memberDisplayName: String
) {
    val memberLeft: Boolean get() = result.reason == GroupMemberRemovalReason.LEFT
}
