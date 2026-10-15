package com.cbgm.sparrow.feature.conversationorchestration.message.domain.policy

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.messagepart.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.OutgoingMessageContent

/** Rules every outgoing message must satisfy before anything is persisted. */
object OutgoingMessageContentPolicy {
    fun requireDirectContent(parts: List<MessagePart>): OutgoingMessageContent {
        val content = requireCommonContent(parts)
        require(content.attachments.none { part -> part.isGroupOnly() }) {
            "Polls and expenses are only supported in group conversations"
        }
        return content
    }

    fun requireGroupContent(parts: List<MessagePart>): OutgoingMessageContent {
        val content = requireCommonContent(parts)
        require(content.attachments.none { part -> part.isGroupOnly() } || content.text.isEmpty()) {
            "Polls and expenses must be sent without a separate text part"
        }
        return content
    }

    private fun requireCommonContent(parts: List<MessagePart>): OutgoingMessageContent {
        require(parts.isNotEmpty()) { "Message must contain message parts" }
        require(parts.map(MessagePart::id).distinct().size == parts.size) {
            "Message part IDs must be unique"
        }

        val textParts = parts.filterIsInstance<Text>()
        require(textParts.size <= 1) { "A message can contain at most one text part" }
        val normalizedText = textParts.singleOrNull()?.text?.trim().orEmpty()
        require(textParts.isEmpty() || normalizedText.isNotEmpty()) {
            "Message text must not be blank"
        }

        val attachments = parts.filterNot { part -> part is Text }
        MessageAttachmentPolicy.requireValid(attachments)
        require(attachments.none { it is Voice } || normalizedText.isEmpty()) {
            "A voice message cannot contain text"
        }
        return OutgoingMessageContent(text = normalizedText, attachments = attachments)
    }

    private fun MessagePart.isGroupOnly(): Boolean =
        this is PaymentTransfer || this is Poll || this is Expense || this is ExpenseBoard || this is ExpenseBoardClosed
}
