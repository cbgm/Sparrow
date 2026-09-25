package com.cbgm.sparrow.feature.chats.presentation.forwarding.mapper

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewContext
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionUiState
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingTargetUi
import com.cbgm.sparrow.feature.contacts.domain.model.Contact

internal fun toForwardingSelectionUiState(
    conversationContext: ConversationOverviewContext,
    contacts: List<Contact>,
    query: String
): ForwardingSelectionUiState {
    val normalizedQuery = query.trim()
    val contactsById = contacts.associateBy(Contact::id)
    val directContactIds =
        conversationContext.conversations
            .asSequence()
            .filter { conversation -> conversation.type == ConversationOverviewType.DIRECT }
            .map { conversation -> conversation.contactId }
            .toSet()

    val chats =
        conversationContext.conversations
            .asSequence()
            .filter { conversation ->
                normalizedQuery.isEmpty() ||
                    conversation.displayName.contains(normalizedQuery, ignoreCase = true) ||
                    (
                        conversation.type == ConversationOverviewType.DIRECT &&
                            contactsById[conversation.contactId]?.matches(normalizedQuery) == true
                    )
            }.sortedByDescending { conversation -> conversation.updatedAtEpochMilliseconds }
            .map { conversation ->
                ForwardingTargetUi(
                    id = "conversation:${conversation.id}",
                    displayName = conversation.displayName,
                    avatarTarget =
                        when (conversation.type) {
                            ConversationOverviewType.DIRECT -> AvatarTarget.User(conversation.contactId)
                            ConversationOverviewType.GROUP -> AvatarTarget.Group(conversation.id)
                        },
                    target =
                        when (conversation.type) {
                            ConversationOverviewType.DIRECT ->
                                ForwardingTarget.Direct(conversation.id)

                            ConversationOverviewType.GROUP ->
                                ForwardingTarget.Group(groupId = conversation.id)
                        }
                )
            }.toList()

    val contactTargets =
        contacts
            .asSequence()
            .filterNot { contact -> contact.id in directContactIds }
            .filter { contact -> contact.matches(normalizedQuery) }
            .sortedBy { contact -> contact.forwardingDisplayName().lowercase() }
            .map { contact ->
                ForwardingTargetUi(
                    id = "contact:${contact.id}",
                    displayName = contact.forwardingDisplayName(),
                    avatarTarget = AvatarTarget.User(contact.id),
                    target = ForwardingTarget.Contact(contact.id)
                )
            }.toList()

    return if (chats.isEmpty() && contactTargets.isEmpty()) {
        ForwardingSelectionUiState.Empty(searchQuery = query)
    } else {
        ForwardingSelectionUiState.Content(
            chats = chats,
            contacts = contactTargets,
            searchQuery = query
        )
    }
}

private fun Contact.matches(query: String): Boolean {
    if (query.isEmpty()) return true

    val normalizedPhoneQuery = query.filter(Char::isDigit)
    val matchesName = displayName?.contains(query, ignoreCase = true) == true
    val matchesPhone =
        normalizedPhoneQuery.isNotEmpty() &&
            phoneNumbers.any { phoneNumber ->
                phoneNumber.value
                    .filter(Char::isDigit)
                    .contains(normalizedPhoneQuery)
            }

    return matchesName || matchesPhone
}

private fun Contact.forwardingDisplayName(): String =
    displayName?.takeIf(String::isNotBlank)
        ?: preferredPhoneNumber?.value
        ?: phoneNumbers.firstOrNull()?.value
        ?: "?"
