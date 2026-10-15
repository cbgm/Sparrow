package com.cbgm.sparrow.feature.chats.data.group.mapper

import com.cbgm.sparrow.core.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType

internal fun String.toChatMessageType(): ChatMessageType =
    when (this) {
        MessageTransportMode.GROUP_MEMBER_ADDED -> ChatMessageType.GROUP_MEMBER_ADDED
        MessageTransportMode.GROUP_MEMBER_REMOVED -> ChatMessageType.GROUP_MEMBER_REMOVED
        MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_REMOVED -> ChatMessageType.LOCAL_GROUP_MEMBERSHIP_REMOVED
        MessageTransportMode.GROUP_MEMBER_LEFT -> ChatMessageType.GROUP_MEMBER_LEFT
        MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_LEFT -> ChatMessageType.LOCAL_GROUP_MEMBERSHIP_LEFT
        else -> ChatMessageType.USER
    }

internal fun String.isLocalMembershipStart(): Boolean = this == MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_STARTED

internal fun String.isLocalMembershipEnd(): Boolean =
    this == MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_REMOVED || this == MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_LEFT

/** Local markers that structure the history but are never shown as messages. */
internal fun String.isHiddenControlMessage(): Boolean =
    this == MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_STARTED || this == MessageTransportMode.LOCAL_CONVERSATION_DELETED
