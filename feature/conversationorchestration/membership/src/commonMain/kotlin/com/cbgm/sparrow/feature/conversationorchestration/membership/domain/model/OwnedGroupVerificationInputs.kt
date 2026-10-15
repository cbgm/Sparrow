package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembershipContext

/** Group verification input snapshot. The keys are copied from Identity and are never DAO entities. */
class OwnedGroupVerificationInputs(
    val membership: GroupVerificationMembershipContext,
    val displayNames: Map<String, String>,
    val pendingIdentities: Map<String, PendingGroupIdentity?>
) {
    fun requireDisplayName(contactId: String): String =
        displayNames[contactId] ?: error("Group verification contact was not resolved: $contactId")
}
