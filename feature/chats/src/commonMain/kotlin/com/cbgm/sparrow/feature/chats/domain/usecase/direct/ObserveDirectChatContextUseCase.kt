package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectChatContext
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.invite.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveDirectChatContextUseCase(
    private val conversationRepository: DirectConversationRepository,
    private val contactRepository: ContactRepository,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(
        conversationId: String,
        contactId: String,
        oldestCursor: MessageHistoryCursor? = null
    ): Flow<DirectChatContext> =
        combine(
            conversationRepository.observe(conversationId, oldestCursor),
            contactRepository
                .observeContacts()
                .map { contacts -> contacts.firstOrNull { contact -> contact.id == contactId } }
                .distinctUntilChanged(),
            identityInvitationRepository.observeState(contactId),
            identitySetupModeRepository.observeMode()
        ) { conversation, contact, handshake, setupMode ->
            DirectChatContext(
                conversation = conversation,
                contact = contact,
                handshake = handshake,
                setupMode = setupMode
            )
        }
}
