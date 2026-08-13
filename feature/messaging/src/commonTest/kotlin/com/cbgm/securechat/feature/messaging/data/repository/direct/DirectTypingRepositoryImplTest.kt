package com.cbgm.securechat.feature.messaging.data.repository.direct

import com.cbgm.securechat.feature.messaging.domain.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayTypingEvent
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class DirectTypingRepositoryImplTest {
    @Test
    fun sendTypingStateResolvesContactBeforeSending() =
        runTest {
            val client = FakeWebSocketTransportClient()
            val gateway =
                DirectTypingRepositoryImpl(
                    webSocketTransportClient = client,
                    contactRelayIdResolver = SuccessfulResolver()
                )

            val result =
                gateway.send(
                    contactId = "contact-1",
                    isTyping = true
                )

            assertTrue(result.isSuccess)
            assertEquals(
                expected = listOf("contact-relay-id" to true),
                actual = client.sentTypingStates
            )
        }

    @Test
    fun resolverFailureIsReturnedWithoutCallingTransport() =
        runTest {
            val expectedError = IllegalStateException("relay ID missing")
            val client = FakeWebSocketTransportClient()
            val gateway =
                DirectTypingRepositoryImpl(
                    webSocketTransportClient = client,
                    contactRelayIdResolver =
                        object : ContactRelayIdResolver {
                            override suspend fun resolve(contactId: String): Result<String> =
                                Result.failure(expectedError)
                        }
                )

            val result =
                gateway.send(
                    contactId = "contact-1",
                    isTyping = true
                )

            assertTrue(result.isFailure)
            assertSame(expectedError, result.exceptionOrNull())
            assertTrue(client.sentTypingStates.isEmpty())
        }

    @Test
    fun observationForwardsRepeatedTypingStatesForTimeoutRecovery() =
        runTest {
            val client = FakeWebSocketTransportClient()
            val gateway =
                DirectTypingRepositoryImpl(
                    webSocketTransportClient = client,
                    contactRelayIdResolver = SuccessfulResolver()
                )
            val observedValues =
                async(start = CoroutineStart.UNDISPATCHED) {
                    gateway
                        .observe(contactId = "contact-1")
                        .take(3)
                        .toList()
                }

            client.incomingEvents.subscriptionCount.first { count -> count > 0 }
            client.incomingEvents.emit(
                RelayTypingEvent(
                    senderId = "different-relay-id",
                    isTyping = true
                )
            )
            client.incomingEvents.emit(
                RelayTypingEvent(
                    senderId = "contact-relay-id",
                    isTyping = true
                )
            )
            client.incomingEvents.emit(
                RelayTypingEvent(
                    senderId = "contact-relay-id",
                    isTyping = true
                )
            )
            client.incomingEvents.emit(
                RelayTypingEvent(
                    senderId = "contact-relay-id",
                    isTyping = false
                )
            )

            assertEquals(
                expected = listOf(true, true, false),
                actual =
                    withTimeout(TEST_TIMEOUT_MILLISECONDS.milliseconds) {
                        observedValues.await()
                    }
            )
        }

    @Test
    fun observationStaysActiveUntilContactRelayIdBecomesAvailable() =
        runTest {
            val client = FakeWebSocketTransportClient()
            val resolver = MutableResolver()
            val gateway =
                DirectTypingRepositoryImpl(
                    webSocketTransportClient = client,
                    contactRelayIdResolver = resolver
                )
            val observedValue =
                async(start = CoroutineStart.UNDISPATCHED) {
                    gateway
                        .observe(contactId = "contact-1")
                        .first()
                }

            client.incomingEvents.subscriptionCount.first { count -> count > 0 }

            client.incomingEvents.emit(
                RelayTypingEvent(
                    senderId = "contact-relay-id",
                    isTyping = true
                )
            )

            resolver.relayId = "contact-relay-id"

            client.incomingEvents.emit(
                RelayTypingEvent(
                    senderId = "contact-relay-id",
                    isTyping = true
                )
            )

            assertTrue(
                withTimeout(TEST_TIMEOUT_MILLISECONDS) {
                    observedValue.await()
                }
            )
        }

    private class SuccessfulResolver : ContactRelayIdResolver {
        override suspend fun resolve(contactId: String): Result<String> =
            Result.success("contact-relay-id")
    }

    private class MutableResolver : ContactRelayIdResolver {
        var relayId: String? = null

        override suspend fun resolve(contactId: String): Result<String> =
            relayId
                ?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("relay ID missing"))
    }

    private class FakeWebSocketTransportClient : WebSocketTransportClient {
        val incomingEvents = MutableSharedFlow<RelayTypingEvent>()
        val sentTypingStates = mutableListOf<Pair<String, Boolean>>()

        override val connectionState: StateFlow<TransportConnectionState> =
            MutableStateFlow(TransportConnectionState.Disconnected)
        override val incomingEnvelopes: Flow<RelayEnvelope> = MutableSharedFlow()
        override val incomingTypingEvents: Flow<RelayTypingEvent> = incomingEvents

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
            return Result.success(Unit)
        }

        override suspend fun disconnect() = Unit
    }

    private companion object {
        const val TEST_TIMEOUT_MILLISECONDS = 1_000L
    }
}
