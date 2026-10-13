package com.cbgm.sparrow.feature.conversationorchestration.api.command

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart

data class ForwardMessage(
    val parts: List<MessagePart>,
    val target: ForwardTarget
) : ConversationCommand

sealed interface ForwardTarget {
    data class DirectConversation(
        val conversationId: String
    ) : ForwardTarget

    data class GroupConversation(
        val groupId: String
    ) : ForwardTarget

    data class Contact(
        val contactId: String
    ) : ForwardTarget
}
