package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization

/**
 * Releasing and discarding a contact's waiting messages share one lock with storing a new waiting message and with
 * editing or deleting a message that was never sent, so a waiting message is never stored, changed or removed while
 * the waiting messages are released. Sends to an authorized contact and changes of sent messages take no lock.
 */
internal fun directReleaseLockKey(contactId: String): String = "direct-release:$contactId"
