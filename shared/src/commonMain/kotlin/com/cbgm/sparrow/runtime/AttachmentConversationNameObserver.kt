package com.cbgm.sparrow.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.combine

/** Publishes Chats-owned display names to Attachments whenever they change, also after restart. */
class AttachmentConversationNameObserver internal constructor(
    private val conversationRepository: ConversationOverviewRepository,
    private val contacts: ContactRepository,
    private val attachments: MessageAttachmentOperationsRepository
) {
    private val logger = SparrowLog.withTag("AttachmentConversationNameObserver")

    suspend fun run() {
        combine(conversationRepository.observeAll(), contacts.observeContacts()) { conversations, knownContacts ->
            val names = knownContacts.associateBy { it.id }
            conversations.map { conversation ->
                val name = if (conversation.type == ConversationOverviewType.DIRECT) {
                    val contact = names[conversation.contactId]
                    contact?.displayName?.takeIf(String::isNotBlank)
                        ?: contact?.preferredPhoneNumber?.value
                        ?: conversation.contactId.ifBlank { conversation.displayName }
                } else {
                    conversation.displayName
                }
                Triple(conversation.id, name.ifBlank { conversation.id }, conversation.type == ConversationOverviewType.GROUP)
            }
        }.collect { conversations ->
            conversations.forEach { (conversationId, displayName, isGroup) ->
                runCatching {
                    attachments.updateConversationDisplayName(
                        conversationId = conversationId,
                        displayName = displayName,
                        isGroup = isGroup
                    )
                }.onFailure { error ->
                    logger.error(error) { "Could not update attachment display name for $conversationId" }
                }
            }
        }
    }
}
