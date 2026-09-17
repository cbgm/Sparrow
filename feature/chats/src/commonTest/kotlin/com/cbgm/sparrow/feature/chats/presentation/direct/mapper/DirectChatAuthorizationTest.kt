package com.cbgm.sparrow.feature.chats.presentation.direct.mapper

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectComposerState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
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
    fun `mutual identity is authorized`() {
        assertTrue(
            isDirectChatAuthorized(
                contact = null,
                identityHandshakeState = IdentityHandshakeState.MUTUAL_UNVERIFIED,
                identitySetupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION
            )
        )
    }

    @Test
    fun `unauthorized conversation can keep composer usable when queueing is available`() {
        val state = directUiStateFor(
            handshake = IdentityHandshakeState.AUTHORIZATION_REVOKED,
            canQueueMessages = true
        )

        assertFalse(state.isChatAuthorized)
        assertEquals(DirectComposerState.QUEUE_ALLOWED, state.composerState)
        assertTrue(state.composerState.isInputEnabled)
        assertTrue(state.composerState.isSendActionEnabled)
        assertFalse(state.composerState.sendsIndicators)
    }

    @Test
    fun `unavailable authorization disables composer`() {
        val state = directUiStateFor(
            handshake = IdentityHandshakeState.ACCEPTANCE_SENT,
            canQueueMessages = false
        )

        assertFalse(state.isChatAuthorized)
        assertEquals(DirectComposerState.DISABLED, state.composerState)
        assertFalse(state.composerState.isInputEnabled)
        assertFalse(state.composerState.isSendActionEnabled)
    }

    @Test
    fun `authorized chat uses ready composer`() {
        val state = directUiStateFor(IdentityHandshakeState.MUTUAL_UNVERIFIED)

        assertTrue(state.isChatAuthorized)
        assertEquals(DirectComposerState.READY, state.composerState)
        assertTrue(state.composerState.sendsIndicators)
    }

    private fun directUiStateFor(
        handshake: IdentityHandshakeState?,
        canQueueMessages: Boolean = false
    ) =
        toDirectConversationUiState(
            contactId = "contact",
            fallbackContactName = "Contact",
            conversation = DirectConversation("conversation", "contact", emptyList(), 0),
            contact = null,
            handshake = handshake,
            canQueueMessages = canQueueMessages,
            setupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION,
            safetyAssessments = emptyMap()
        )
}
