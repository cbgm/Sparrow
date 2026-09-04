package com.cbgm.sparrow.feature.chats.presentation.direct.mapper

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectComposerState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectChatAuthorizationTest {
    @Test
    fun `acceptance sent is not yet authorized`() {
        assertFalse(
            isDirectChatAuthorized(
                contact = null,
                identityHandshakeState = IdentityHandshakeState.ACCEPTANCE_SENT,
                identitySetupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION
            )
        )
    }

    @Test
    fun `waiting for ready is authorized after acceptance completed`() {
        assertTrue(
            isDirectChatAuthorized(
                contact = null,
                identityHandshakeState = IdentityHandshakeState.WAITING_FOR_READY,
                identitySetupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION
            )
        )
    }

    @Test
    fun `mutual invitation is authorized`() {
        assertTrue(
            isDirectChatAuthorized(
                contact = null,
                identityHandshakeState = IdentityHandshakeState.MUTUAL_UNVERIFIED,
                identitySetupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION
            )
        )
    }

    @Test
    fun `deleted peer conversation requires reinvite but keeps composer editable`() {
        val state = directUiStateFor(IdentityHandshakeState.CONVERSATION_DELETED)

        assertFalse(state.isChatAuthorized)
        assertEquals(DirectComposerState.REINVITE_REQUIRED, state.composerState)
        assertTrue(state.composerState.isInputEnabled)
        assertTrue(state.composerState.isSendActionEnabled)
        assertFalse(state.composerState.sendsTypingIndicators)
    }

    @Test
    fun `pending reinvite keeps composer usable for locally queued messages`() {
        val state = directUiStateFor(IdentityHandshakeState.INVITE_SENT)

        assertFalse(state.isChatAuthorized)
        assertEquals(DirectComposerState.REINVITE_PENDING, state.composerState)
        assertTrue(state.composerState.isInputEnabled)
        assertTrue(state.composerState.isSendActionEnabled)
        assertFalse(state.composerState.sendsTypingIndicators)
    }

    @Test
    fun `failed reinvite can be started again by another send action`() {
        val state = directUiStateFor(IdentityHandshakeState.FAILED)

        assertEquals(DirectComposerState.REINVITE_REQUIRED, state.composerState)
        assertTrue(state.composerState.isInputEnabled)
        assertTrue(state.composerState.isSendActionEnabled)
    }

    @Test
    fun `authorized chat uses ready composer`() {
        val state = directUiStateFor(IdentityHandshakeState.MUTUAL_UNVERIFIED)

        assertTrue(state.isChatAuthorized)
        assertEquals(DirectComposerState.READY, state.composerState)
        assertTrue(state.composerState.sendsTypingIndicators)
    }

    private fun directUiStateFor(handshake: IdentityHandshakeState) =
        toDirectConversationUiState(
            contactId = "contact",
            fallbackContactName = "Contact",
            conversation = DirectConversation("conversation", "contact", emptyList(), 0),
            contact = null,
            handshake = handshake,
            setupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION,
            safetyAssessments = emptyMap()
        )
}
