package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization

/** Releasing and discarding a contact's waiting messages share one lock so they never run at the same time. */
internal fun directReleaseLockKey(contactId: String): String = "direct-release:$contactId"
