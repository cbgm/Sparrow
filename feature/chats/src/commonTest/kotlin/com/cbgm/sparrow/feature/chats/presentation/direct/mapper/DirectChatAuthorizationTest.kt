package com.cbgm.sparrow.feature.chats.presentation.direct.mapper

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectComposerState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectChatAuthorizationTest {
    @Test
    fun `acceptance sent is not yet authorized`() {
        assertFalse(
            isDirectChatAuthorized(
                remoteIdentity = null,
                identityHandshakeState = IdentityHandshakeState.ACCEPTANCE_SENT,
                identitySetupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION
            )
        )
    }

    @Test
    fun `waiting for ready is authorized after acceptance completed`() {
        assertTrue(
            isDirectChatAuthorized(
                remoteIdentity = null,
                identityHandshakeState = IdentityHandshakeState.WAITING_FOR_READY,
                identitySetupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION
            )
        )
    }

    @Test
    fun `mutual identity is authorized`() {
        assertTrue(
            isDirectChatAuthorized(
                remoteIdentity = null,
                identityHandshakeState = IdentityHandshakeState.MUTUAL_UNVERIFIED,
                identitySetupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION
            )
        )
    }

    @Test
    fun `unauthorized conversation can keep composer usable when queueing is available`() {
        val state = directUiStateFor(
            handshake = IdentityHandshakeState.EXCHANGE_INVALIDATED,
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

    @Test
    fun `manual invitation does not authorize or import the peer identity`() {
        val handshake = IdentityHandshakeState.MUTUAL_UNVERIFIED
        val invitationOnlyIdentity = identity(KeyExchangeStatus.MUTUAL, locallyImported = false)
        assertFalse(isDirectChatAuthorized(invitationOnlyIdentity, handshake, DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING))
        val state = toDirectConversationUiState(
            contactId = "contact",
            fallbackContactName = "Contact",
            conversation = DirectConversation("conversation", "contact", emptyList(), 0),
            contact = null,
            remoteIdentity = invitationOnlyIdentity,
            handshake = handshake,
            canQueueMessages = false,
            setupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
            safetyAssessments = emptyMap()
        )
        assertEquals(ContactSecurityState.NO_REMOTE_PUBLIC_KEYS, state.contactSecurityState)
        assertFalse(state.isChatAuthorized)
    }

    @Test
    fun `manual setup requires explicit import and established invitation`() {
        val oneWay = identity(KeyExchangeStatus.ONE_WAY)
        val mutual = identity(KeyExchangeStatus.MUTUAL)
        val handshake = IdentityHandshakeState.MUTUAL_UNVERIFIED
        assertFalse(isDirectChatAuthorized(oneWay, handshake, DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING))
        assertFalse(isDirectChatAuthorized(mutual, null, DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING))
        assertTrue(isDirectChatAuthorized(mutual, handshake, DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING))
    }

    @Test
    fun `identity verification state is mapped from Identity owned data`() {
        assertEquals(
            ContactSecurityState.MUTUAL_KEYS_UNVERIFIED,
            identity(KeyExchangeStatus.MUTUAL).toContactSecurityState()
        )
        assertEquals(
            ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME,
            identity(KeyExchangeStatus.MUTUAL, ContactVerificationStatus.VERIFIED).toContactSecurityState()
        )
    }

    private fun identity(
        exchange: KeyExchangeStatus,
        verification: ContactVerificationStatus = ContactVerificationStatus.UNVERIFIED,
        locallyImported: Boolean = true
    ) = RemotePeerIdentity(
        peerId = "contact",
        encryptionPublicKey = byteArrayOf(1),
        signingPublicKey = byteArrayOf(2),
        verificationStatus = verification,
        keyExchangeStatus = exchange,
        verifiedByContact = false,
        locallyImported = locallyImported,
        updatedAtEpochMilliseconds = 1L
    )

    private fun directUiStateFor(
        handshake: IdentityHandshakeState?,
        canQueueMessages: Boolean = false
    ) =
        toDirectConversationUiState(
            contactId = "contact",
            fallbackContactName = "Contact",
            conversation = DirectConversation("conversation", "contact", emptyList(), 0),
            contact = null,
            remoteIdentity = null,
            handshake = handshake,
            canQueueMessages = canQueueMessages,
            setupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION,
            safetyAssessments = emptyMap()
        )
}
