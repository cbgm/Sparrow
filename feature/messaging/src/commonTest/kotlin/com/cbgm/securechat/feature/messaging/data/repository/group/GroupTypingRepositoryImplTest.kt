package com.cbgm.securechat.feature.messaging.data.repository.group

import com.cbgm.securechat.feature.messaging.application.routing.GroupRoutingIdResolver
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.gateway.model.GatewayTypingEvent
import com.cbgm.securechat.feature.transport.gateway.model.TransportEnvelope
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupTypingRepositoryImplTest {
    @Test
    fun setTypingAttemptsEveryCurrentMemberWhenOneSendFails() =
        runTest {
            val client = FakeWebSocketTransportClient(failingRecipientId = "group-1-contact-1-routing")
            val repository =
                GroupTypingRepositoryImpl(
                    webSocketTransportClient = client,
                    groupRoutingIdResolver = GroupIdResolver()
                )

            val result =
                repository.setTyping(
                    groupId = "group-1",
                    isTyping = true
                )

            assertTrue(result.isFailure)
            assertEquals(
                expected =
                    listOf(
                        "group-1-contact-1-routing" to true,
                        "group-1-contact-2-routing" to true
                    ),
                actual = client.sentTypingStates
            )
        }

    @Test
    fun observeMemberMatchesCanonicalGroupRoutingId() =
        runTest {
            val client = FakeWebSocketTransportClient()
            val repository =
                GroupTypingRepositoryImpl(
                    webSocketTransportClient = client,
                    groupRoutingIdResolver = GroupIdResolver()
                )
            val observed = async { repository.observeMember("group-1", "contact-2").first() }
            runCurrent()

            client.typingEvents.emit(
                GatewayTypingEvent(
                    senderId = "group-1-contact-2-routing",
                    isTyping = true
                )
            )

            assertTrue(observed.await())
        }

    private class GroupIdResolver : GroupRoutingIdResolver {
        override suspend fun resolve(
            groupId: String,
            contactId: String
        ): Result<String> = Result.success("$groupId-$contactId-routing")

        override suspend fun resolveMembers(groupId: String): Result<Map<String, String>> =
            Result.success(
                linkedMapOf(
                    "contact-1" to "$groupId-contact-1-routing",
                    "contact-2" to "$groupId-contact-2-routing"
                )
            )

        override fun resolveRemovedMember(signingPublicKey: ByteArray): Result<String> =
            Result.success("removed-member-routing")

        override suspend fun resolveForMessage(
            messageId: String,
            contactId: String
        ): Result<String?> = Result.success(null)

        override suspend fun resolveContactId(routingId: String): Result<String?> =
            Result.success(null)
    }

    private class FakeWebSocketTransportClient(
        private val failingRecipientId: String? = null
    ) : WebSocketTransportClient {
        val sentTypingStates = mutableListOf<Pair<String, Boolean>>()
        val typingEvents = MutableSharedFlow<GatewayTypingEvent>()

        override val connectionState: StateFlow<TransportConnectionState> =
            MutableStateFlow(TransportConnectionState.Disconnected)
        override val incomingEnvelopes: Flow<TransportEnvelope> = MutableSharedFlow()
        override val incomingTypingEvents: Flow<GatewayTypingEvent> = typingEvents

        override fun connect(
            serverUrl: String,
            localRoutingId: String
        ) = Unit

        override suspend fun sendEnvelopeAndAwaitAcceptance(
            envelope: TransportEnvelope,
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
