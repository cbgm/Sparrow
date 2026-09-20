package com.cbgm.sparrow.feature.attachments.domain.model

/** The Chats owner supplies the context of a message before attaching its files. */
data class AttachmentMessageContext(
    val conversationId: String,
    val createdAtEpochMilliseconds: Long,
    val displayName: String,
    val isGroup: Boolean,
    val isMine: Boolean,
    val senderContactId: String?
)
