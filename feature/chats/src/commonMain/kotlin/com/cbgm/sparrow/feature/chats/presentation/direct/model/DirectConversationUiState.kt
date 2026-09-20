package com.cbgm.sparrow.feature.chats.presentation.direct.model

import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectComposerState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode

data class DirectConversationUiState(
    val contactId: String = "",
    val contactName: String = "",
    val messages: List<MessageBubbleUi> = emptyList(),
    val contactSecurityState: ContactSecurityState = ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
    val identitySetupMode: DirectIdentitySetupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
    val isLoading: Boolean = true,
    val isChatAuthorized: Boolean = false,
    val composerState: DirectComposerState = DirectComposerState.DISABLED
)

fun DirectConversationUiState.findMessage(id: String?) =
    messages.firstOrNull { it.id == id }
