package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectChatContext
import com.cbgm.sparrow.feature.chats.domain.usecase.profile.ObserveRemoteProfilePicturesUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentitySetupModeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveDirectChatContextUseCase(
    private val observeConversation: ObserveDirectConversationUseCase,
    private val observeContact: ObserveContactUseCase,
    private val observeIdentityHandshakeState: ObserveIdentityHandshakeStateUseCase,
    private val observeIdentitySetupMode: ObserveIdentitySetupModeUseCase,
    private val observeProfilePictures: ObserveRemoteProfilePicturesUseCase
) {
    operator fun invoke(
        conversationId: String,
        contactId: String
    ): Flow<DirectChatContext> =
        combine(
            observeConversation(conversationId),
            observeContact(contactId),
            observeIdentityHandshakeState(contactId),
            observeIdentitySetupMode(),
            observeProfilePictures(contactId)
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
