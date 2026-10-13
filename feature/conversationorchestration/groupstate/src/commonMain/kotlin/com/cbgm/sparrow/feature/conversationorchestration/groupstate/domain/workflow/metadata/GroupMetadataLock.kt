package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

/** Title, description and avatar changes of one group are serialized. */
internal fun groupMetadataLockKey(groupId: String): String = "group-metadata:$groupId"
