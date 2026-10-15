package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

/** All workflows that hand group messages to the outbox share one lock per group to keep ordering. */
internal fun groupSendLockKey(groupId: String): String = "group-send:$groupId"
