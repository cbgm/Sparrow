package com.cbgm.sparrow.feature.chats.domain.model.group

/** Identity of the message being pinned; Chats owns the message and its sender metadata. */
data class GroupPinTarget(
    val isMine: Boolean,
    val senderContactId: String?,
    val isAlreadyPinned: Boolean
)
