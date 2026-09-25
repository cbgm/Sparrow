package com.cbgm.sparrow.feature.chats.domain.model

sealed interface ForwardingTarget {
    data class Direct(
        val conversationId: String
    ) : ForwardingTarget

    data class Group(
        val groupId: String
    ) : ForwardingTarget

    data class Contact(
        val contactId: String
    ) : ForwardingTarget
}
