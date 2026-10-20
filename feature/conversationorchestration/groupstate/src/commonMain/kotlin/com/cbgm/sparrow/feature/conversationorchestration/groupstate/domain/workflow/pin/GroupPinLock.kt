package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

/** Pin, unpin and expense board changes of one group are serialized. */
internal fun groupPinLockKey(groupId: String): String = "group-pin:$groupId"
