package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification

/** All verification changes of one group are serialized, including incoming verification packets. */
fun groupVerificationLockKey(groupId: String): String = "group-verification:$groupId"
