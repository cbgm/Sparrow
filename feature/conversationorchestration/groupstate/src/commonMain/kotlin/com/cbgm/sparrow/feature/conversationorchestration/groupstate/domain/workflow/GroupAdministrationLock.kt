package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow

/**
 * Serializes everything an admin publishes to the members of one group: membership changes that rotate the epoch
 * (add, remove, promote, transfer, leave, delete) and the publishing of the signed group state (title, description,
 * avatar, pin, expense board closing, the replay to a new member). A metadata packet is therefore always signed for
 * the epoch and recipients that are current when it is queued; a removal cannot rotate the epoch in between. A local
 * title, description, avatar or pin change is saved before and outside the lock, so the user never waits for it.
 *
 * The lock is not reentrant: none of these workflows runs another one of them nested.
 */
fun groupAdministrationLockKey(groupId: String): String = "group-administration:$groupId"
