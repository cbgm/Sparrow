package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

/** Membership changes of one group are serialized. */
internal fun groupAdministrationLockKey(groupId: String): String = "group-administration:$groupId"
