package com.cbgm.sparrow.feature.chats.presentation.direct.model

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState

data class DirectUiState(
    val contactId: String = "",
    val contactName: String = "",
    val messages: List<MessageBubbleModel> = emptyList(),
    val messageText: String = "",
    val isContactTyping: Boolean = false,
    val contactSecurityState: ContactSecurityState = ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
    val identityHandshakeState: IdentityHandshakeState? = null,
    val identitySetupMode: DirectIdentitySetupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
    val isLoading: Boolean = true,
    val isMessageInputEnabled: Boolean = false,
    val errorMessage: String? = null
)
