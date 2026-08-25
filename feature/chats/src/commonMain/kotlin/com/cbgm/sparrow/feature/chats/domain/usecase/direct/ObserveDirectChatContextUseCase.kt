package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectChatContext
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class ObserveDirectChatContextUseCase(
    private val conversationRepository: DirectConversationRepository,
    private val contactRepository: ContactRepository,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository,
    private val remoteProfilePictureProvider: RemoteProfilePictureProvider
) {
    operator fun invoke(
        conversationId: String,
        contactId: String
    ): Flow<DirectChatContext> =
        combine(
            conversationRepository.observe(conversationId),
            contactRepository
                .observeContacts()
                .map { contacts -> contacts.firstOrNull { contact -> contact.id == contactId } }
                .distinctUntilChanged(),
            identityInvitationRepository.observeState(contactId),
            identitySetupModeRepository.observeMode(),
            remoteProfilePictureProvider
                .observe(contactId)
                .map { picture -> picture.bytes }
                .catch { emit(null) }
                .onStart { emit(null) }
        ) { conversation, contact, handshake, setupMode, profilePictureBytes ->
            DirectChatContext(
                conversation = conversation,
                contact = contact,
                handshake = handshake,
                setupMode = setupMode,
                profilePictureBytes = profilePictureBytes
            )
        }
}
