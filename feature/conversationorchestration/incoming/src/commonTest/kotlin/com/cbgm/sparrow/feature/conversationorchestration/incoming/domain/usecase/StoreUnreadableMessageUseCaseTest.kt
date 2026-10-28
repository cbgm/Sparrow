package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.crypto.hash.DefaultCryptoHash
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation.FakeDirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation.MessageStore
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * An unreadable envelope is shown only in the existing chat of a contact the router accepts, and a redelivered
 * envelope does not add a second placeholder.
 */
class StoreUnreadableMessageUseCaseTest {
    private val identities = ParkedEnvelopesTest.FakeIdentities()
    private val conversations = FakeConversations()
    private val store = MessageStore()
    private val storeUnreadable =
        StoreUnreadableMessageUseCase(
            resolveDirectPeerAccess =
                ResolveDirectPeerAccessUseCase(
                    GetIdentityPeerStateUseCase(identities, ParkedEnvelopesTest.NoPendingChanges()),
                    ParkedEnvelopesTest.FakeBlocklist()
                ),
            conversationRepository = conversations,
            messageRepository = FakeDirectMessageRepository(store),
            cryptoHash = DefaultCryptoHash()
        )

    @Test
    fun authorizedContactGetsOnePlaceholderInItsChatEvenWhenTheEnvelopeIsRedelivered() = runBlocking {
        identities.states[CONTACT_ID] = IdentityPeerState(true, false)
        conversations.ids[CONTACT_ID] = CONVERSATION_ID

        assertTrue(store(PAYLOAD, receivedAt = 1L))
        assertFalse(store(PAYLOAD, receivedAt = 2L))

        val placeholder = store.messages.values.single()
        assertEquals(CONVERSATION_ID, placeholder.conversationId)
        assertEquals(1L, placeholder.createdAtEpochMilliseconds)
        assertTrue(placeholder.id.startsWith("failed-message-"))
        assertTrue(store(OTHER_PAYLOAD, receivedAt = 3L))
        assertEquals(2, store.messages.size)
    }

    @Test
    fun authorizedContactWithoutChatGetsNoNewChat() = runBlocking {
        identities.states[CONTACT_ID] = IdentityPeerState(true, false)

        assertFalse(store(PAYLOAD, receivedAt = 1L))
        assertTrue(store.messages.isEmpty())
    }

    @Test
    fun groupOnlyMemberAndPendingContactAreDropped() = runBlocking {
        conversations.ids[CONTACT_ID] = CONVERSATION_ID

        assertFalse(store(PAYLOAD, receivedAt = 1L))
        identities.states[CONTACT_ID] = IdentityPeerState(false, false, hasPendingExchange = true)
        assertFalse(store(PAYLOAD, receivedAt = 2L))
        assertTrue(store.messages.isEmpty())
    }

    private suspend fun store(payload: String, receivedAt: Long): Boolean =
        storeUnreadable(
            contactId = CONTACT_ID,
            encodedTransportPayload = payload,
            text = "Unable to decrypt secure message",
            transportMode = "SEALED_BOX",
            contentStatus = MessageContentStatus.TRANSPORT_DECRYPTION_FAILED,
            receivedAtEpochMilliseconds = receivedAt
        ).getOrThrow()

    /** Lookup only: creating a chat for an unreadable envelope fails the test. */
    private class FakeConversations : DirectConversationRepository {
        val ids = mutableMapOf<String, String>()

        override suspend fun getOrCreate(contactId: String): Result<String> = error(NOT_USED)

        override suspend fun findConversationId(contactId: String): Result<String?> = Result.success(ids[contactId])

        override suspend fun findContactId(conversationId: String): Result<String?> = error(NOT_USED)

        override suspend fun requireTarget(conversationId: String): Result<DirectConversationTarget> = error(NOT_USED)

        override suspend fun delete(conversationId: String): Result<Unit> = error(NOT_USED)
    }

    private companion object {
        const val NOT_USED = "Not used by the unreadable message store"
        const val CONTACT_ID = "alice"
        const val CONVERSATION_ID = "chat-alice"
        const val PAYLOAD = "sealed-payload"
        const val OTHER_PAYLOAD = "other-sealed-payload"
    }
}
