package com.cbgm.sparrow.feature.chats.presentation.details.model

sealed interface DetailsTarget {
    data class Contact(
        val contactId: String
    ) : DetailsTarget

    data class Group(
        val conversationId: String
    ) : DetailsTarget
}
