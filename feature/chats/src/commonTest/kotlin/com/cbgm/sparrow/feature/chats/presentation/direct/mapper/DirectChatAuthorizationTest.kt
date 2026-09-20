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
                identityHandshakeState = IdentityHandshakeState.ACCEPTANCE_SENT
            )
        )
    }

    @Test
    fun `waiting for ready is authorized after acceptance completed`() {
        assertTrue(
            isDirectChatAuthorized(
                identityHandshakeState = IdentityHandshakeState.WAITING_FOR_READY
            )
        )
    }

    @Test
    fun `mutual identity is authorized`() {
        assertTrue(
            isDirectChatAuthorized(
                identityHandshakeState = IdentityHandshakeState.MUTUAL_UNVERIFIED
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
    fun `manual acceptance enables writing and sending without imported identity`() {
        val state = toDirectConversationUiState(
            contactId = "contact",
            fallbackContactName = "Contact",
            conversation = DirectConversation("conversation", "contact", emptyList(), 0),
            contact = null,
            remoteIdentity = null,
            handshake = IdentityHandshakeState.WAITING_FOR_READY,
            canQueueMessages = false,
            setupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
            safetyAssessments = emptyMap()
        )

        assertTrue(state.isChatAuthorized)
        assertEquals(DirectComposerState.READY, state.composerState)
        assertTrue(state.composerState.isInputEnabled)
        assertTrue(state.composerState.isSendActionEnabled)
        assertEquals(ContactSecurityState.NO_REMOTE_PUBLIC_KEYS, state.contactSecurityState)
        assertFalse(state.isLoading)
    }

    @Test
    fun `manual identity import without accepted invitation does not authorize chat`() {
        assertFalse(isDirectChatAuthorized(null))
        assertFalse(isDirectChatAuthorized(IdentityHandshakeState.ACCEPTANCE_SENT))
        assertTrue(isDirectChatAuthorized(IdentityHandshakeState.MUTUAL_UNVERIFIED))
    }

    @Test
    fun `automatic and manual acceptance have the same composer state`() {
        for (mode in DirectIdentitySetupMode.entries) {
            val state = toDirectConversationUiState(
                contactId = "contact",
                fallbackContactName = "Contact",
                conversation = DirectConversation("conversation", "contact", emptyList(), 0),
                contact = null,
                remoteIdentity = null,
                handshake = IdentityHandshakeState.WAITING_FOR_READY,
                canQueueMessages = false,
                setupMode = mode,
                safetyAssessments = emptyMap()
            )
            assertEquals(DirectComposerState.READY, state.composerState)
        }
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

    @Test
    fun `manual share with no imported peer identity shows incomplete not setup`() {
        assertEquals(
            ContactSecurityState.LOCAL_IDENTITY_SHARED,
            resolveDirectSecurityState(
                identity = null,
                setupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
                localIdentityShared = true,
                handshake = IdentityHandshakeState.WAITING_FOR_READY
            )
        )
    }

    @Test
    fun `import alone does not claim mutual exchange or local share`() {
        assertEquals(
            ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
            resolveDirectSecurityState(
                identity = identity(KeyExchangeStatus.ONE_WAY, locallyImported = true),
                setupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
                localIdentityShared = false,
                handshake = IdentityHandshakeState.WAITING_FOR_READY
            )
        )
    }

    @Test
    fun `share followed by import remains incomplete until mutual keys are established`() {
        assertEquals(
            ContactSecurityState.ONE_WAY_KEYS,
            resolveDirectSecurityState(
                identity = identity(KeyExchangeStatus.ONE_WAY, locallyImported = true),
                setupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
                localIdentityShared = true,
                handshake = IdentityHandshakeState.WAITING_FOR_READY
            )
        )
    }

    @Test
    fun `persisted mutual identity restores verify state without resending invitation`() {
        assertEquals(
            ContactSecurityState.MUTUAL_KEYS_UNVERIFIED,
            resolveDirectSecurityState(
                identity = identity(KeyExchangeStatus.MUTUAL, locallyImported = true),
                setupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
                localIdentityShared = false,
                handshake = IdentityHandshakeState.WAITING_FOR_READY
            )
        )
    }

    @Test
    fun `automatic invitation acceptance shows incomplete while remote identity is missing`() {
        assertEquals(
            ContactSecurityState.LOCAL_IDENTITY_SHARED,
            resolveDirectSecurityState(
                identity = null,
                setupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION,
                localIdentityShared = false,
                handshake = IdentityHandshakeState.WAITING_FOR_READY
            )
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
