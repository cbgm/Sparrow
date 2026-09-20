package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectChatContext
import com.cbgm.sparrow.feature.chats.domain.model.direct.hasSameIdentityContent
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ObserveConversationQueueAvailabilityUseCase
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalIdentitySharedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveRemoteIdentitiesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveDirectChatContextUseCase(
    private val conversationRepository: DirectConversationRepository,
    private val contactRepository: ContactRepository,
    private val observeIdentityHandshakeState: ObserveIdentityHandshakeStateUseCase,
    private val observeRemoteIdentities: ObserveRemoteIdentitiesUseCase,
    private val observeLocalIdentityShared: ObserveLocalIdentitySharedUseCase,
    private val observeConversationQueueAvailability: ObserveConversationQueueAvailabilityUseCase,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(
        conversationId: String,
        contactId: String,
        oldestCursor: MessageHistoryCursor? = null
    ): Flow<DirectChatContext> =
        combine(
            combine(
                conversationRepository.observe(conversationId, oldestCursor),
                contactRepository
                    .observeContacts()
                    .map { contacts -> contacts.firstOrNull { contact -> contact.id == contactId } }
                    .distinctUntilChanged(),
                observeIdentityHandshakeState(contactId),
                observeConversationQueueAvailability(contactId),
                identitySetupModeRepository.observeMode()
            ) { conversation, contact, handshake, canQueueMessages, setupMode ->
                DirectChatContext(
                    conversation = conversation,
                    contact = contact,
                    handshake = handshake,
                    canQueueMessages = canQueueMessages,
                    setupMode = setupMode
                )
            },
            combine(
                observeRemoteIdentities()
                    .map { identities -> identities.firstOrNull { it.peerId == contactId } }
                    .distinctUntilChanged { previous, current ->
                        previous.hasSameIdentityContent(current)
                    },
                observeLocalIdentityShared(contactId)
            ) { remoteIdentity, localIdentityShared -> remoteIdentity to localIdentityShared }
        ) { chatContext, (remoteIdentity, localIdentityShared) ->
            chatContext.copy(remoteIdentity = remoteIdentity, localIdentityShared = localIdentityShared)
        }
}
