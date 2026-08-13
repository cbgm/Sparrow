package com.cbgm.securechat.feature.messaging.data.repository.group

import com.cbgm.securechat.feature.messaging.domain.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayTypingEvent
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupTypingRepositoryImplTest {
    @Test
    fun sendToMembersAttemptsEveryMemberWhenOneSendFails() =
        runTest {
            val client = FakeWebSocketTransportClient(failingRecipientId = "contact-1-relay")
            val repository =
                GroupTypingRepositoryImpl(
                    webSocketTransportClient = client,
                    contactRelayIdResolver = ContactIdResolver()
                )

            val result =
                repository.sendToMembers(
                    contactIds = linkedSetOf("contact-1", "contact-2"),
                    isTyping = true
                )

            assertTrue(result.isFailure)
            assertEquals(
                expected =
                    listOf(
                        "contact-1-relay" to true,
                        "contact-2-relay" to true
                    ),
                actual = client.sentTypingStates
            )
        }

    private class ContactIdResolver : ContactRelayIdResolver {
        override suspend fun resolve(contactId: String): Result<String> =
            Result.success("$contactId-relay")
    }

    private class FakeWebSocketTransportClient(
        private val failingRecipientId: String
    ) : WebSocketTransportClient {
        val sentTypingStates = mutableListOf<Pair<String, Boolean>>()

        override val connectionState: StateFlow<TransportConnectionState> =
            MutableStateFlow(TransportConnectionState.Disconnected)
        override val incomingEnvelopes: Flow<RelayEnvelope> = MutableSharedFlow()
        override val incomingTypingEvents: Flow<RelayTypingEvent> = MutableSharedFlow()

        override fun connect(
            serverUrl: String,
            localRelayId: String
        ) = Unit

        override suspend fun sendEnvelopeAndAwaitAcceptance(
            envelope: RelayEnvelope,
            timeoutMilliseconds: Long
        ): Result<Unit> = Result.success(Unit)

        override suspend fun acknowledgeIncomingEnvelope(envelopeId: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun sendTypingState(
            recipientId: String,
            isTyping: Boolean
        ): Result<Unit> {
            sentTypingStates += recipientId to isTyping
            return if (recipientId == failingRecipientId) {
                Result.failure(IllegalStateException("typing send failed"))
            } else {
                Result.success(Unit)
            }
        }

        override suspend fun disconnect() = Unit
    }
}
