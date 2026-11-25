package com.cbgm.sparrow.protocol.attachment

import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import kotlinx.coroutines.flow.Flow

/** The attachment parts of messages, by message ID, for the module that renders the conversation. */
interface MessageAttachmentPartSource {
    /** Chats owns paging and supplies the visible message IDs. */
    fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePart>>>
}
