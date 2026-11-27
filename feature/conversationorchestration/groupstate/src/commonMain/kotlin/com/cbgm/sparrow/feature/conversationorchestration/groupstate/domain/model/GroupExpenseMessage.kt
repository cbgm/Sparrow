package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage

/** A stored message of an expense board with its text and parts, ready to be encrypted again. */
data class GroupExpenseMessage(
    val message: StoredMessage,
    val text: String,
    val parts: List<MessagePart>
)
