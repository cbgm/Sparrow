package com.cbgm.securechat.feature.messaging.data.repository.direct

import com.cbgm.securechat.feature.messaging.application.routing.ContactRoutingIdResolver
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.gateway.model.GatewayTypingEvent
import com.cbgm.securechat.feature.transport.gateway.model.TransportEnvelope
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
                    contactRoutingIdResolver = SuccessfulResolver()
                )

            val result =
                gateway.send(
                    contactId = "contact-1",
                    isTyping = true
                )

            assertTrue(result.isSuccess)
            assertEquals(
                expected = listOf("contact-routing-id" to true),
                actual = client.sentTypingStates
            )
        }

    @Test
    fun resolverFailureIsReturnedWithoutCallingTransport() =
        runTest {
            val expectedError = IllegalStateException("routing ID missing")
            val client = FakeWebSocketTransportClient()
            val gateway =
                DirectTypingRepositoryImpl(
                    webSocketTransportClient = client,
                    contactRoutingIdResolver =
                        object : ContactRoutingIdResolver {
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
                    contactRoutingIdResolver = SuccessfulResolver()
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
                GatewayTypingEvent(
                    senderId = "different-routing-id",
                    isTyping = true
                )
            )
            client.incomingEvents.emit(
                GatewayTypingEvent(
                    senderId = "contact-routing-id",
                    isTyping = true
                )
            )
            client.incomingEvents.emit(
                GatewayTypingEvent(
                    senderId = "contact-routing-id",
                    isTyping = true
                )
            )
            client.incomingEvents.emit(
                GatewayTypingEvent(
                    senderId = "contact-routing-id",
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
    fun observationStaysActiveUntilContactRoutingIdBecomesAvailable() =
        runTest {
            val client = FakeWebSocketTransportClient()
            val resolver = MutableResolver()
            val gateway =
                DirectTypingRepositoryImpl(
                    webSocketTransportClient = client,
                    contactRoutingIdResolver = resolver
                )
            val observedValue =
                async(start = CoroutineStart.UNDISPATCHED) {
                    gateway
                        .observe(contactId = "contact-1")
                        .first()
                }

            client.incomingEvents.subscriptionCount.first { count -> count > 0 }

            client.incomingEvents.emit(
                GatewayTypingEvent(
                    senderId = "contact-routing-id",
                    isTyping = true
                )
            )

            resolver.routingId = "contact-routing-id"

            client.incomingEvents.emit(
                GatewayTypingEvent(
                    senderId = "contact-routing-id",
                    isTyping = true
                )
            )

            assertTrue(
                withTimeout(TEST_TIMEOUT_MILLISECONDS) {
                    observedValue.await()
                }
            )
        }

    private class SuccessfulResolver : ContactRoutingIdResolver {
        override suspend fun resolve(contactId: String): Result<String> =
            Result.success("contact-routing-id")
    }

    private class MutableResolver : ContactRoutingIdResolver {
        var routingId: String? = null

        override suspend fun resolve(contactId: String): Result<String> =
            routingId
                ?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("routing ID missing"))
    }

    private class FakeWebSocketTransportClient : WebSocketTransportClient {
        val incomingEvents = MutableSharedFlow<GatewayTypingEvent>()
        val sentTypingStates = mutableListOf<Pair<String, Boolean>>()

        override val connectionState: StateFlow<TransportConnectionState> =
            MutableStateFlow(TransportConnectionState.Disconnected)
        override val incomingEnvelopes: Flow<TransportEnvelope> = MutableSharedFlow()
        override val incomingTypingEvents: Flow<GatewayTypingEvent> = incomingEvents

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
            return Result.success(Unit)
        }

        override suspend fun disconnect() = Unit
    }

    private companion object {
        const val TEST_TIMEOUT_MILLISECONDS = 1_000L
    }
}
