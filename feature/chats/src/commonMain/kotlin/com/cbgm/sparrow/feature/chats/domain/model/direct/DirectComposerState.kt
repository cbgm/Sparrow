package com.cbgm.sparrow.feature.chats.domain.model.direct

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState

enum class DirectComposerState(
    val isInputEnabled: Boolean,
    val isSendActionEnabled: Boolean,
    val sendsTypingIndicators: Boolean
) {
    READY(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsTypingIndicators = true
    ),
    REINVITE_REQUIRED(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsTypingIndicators = false
    ),
    REINVITE_PENDING(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsTypingIndicators = false
    ),
    DISABLED(
        isInputEnabled = false,
        isSendActionEnabled = false,
        sendsTypingIndicators = false
    )
}

fun resolveDirectComposerState(
    hasConversation: Boolean,
    isChatAuthorized: Boolean,
    handshake: IdentityHandshakeState?,
    setupMode: DirectIdentitySetupMode
): DirectComposerState {
    if (!hasConversation) return DirectComposerState.DISABLED

    return when (setupMode) {
        DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
            if (isChatAuthorized) DirectComposerState.READY else DirectComposerState.DISABLED

        DirectIdentitySetupMode.AUTOMATIC_INVITATION ->
            when {
                isChatAuthorized -> DirectComposerState.READY
                handshake == IdentityHandshakeState.INVITE_SENT -> DirectComposerState.REINVITE_PENDING
                handshake in REINVITE_RETRY_STATES -> DirectComposerState.REINVITE_REQUIRED
                else -> DirectComposerState.DISABLED
            }
    }
}

private val REINVITE_RETRY_STATES =
    setOf(
        IdentityHandshakeState.CONVERSATION_DELETED,
        IdentityHandshakeState.DECLINED,
        IdentityHandshakeState.EXPIRED,
        IdentityHandshakeState.FAILED
    )
