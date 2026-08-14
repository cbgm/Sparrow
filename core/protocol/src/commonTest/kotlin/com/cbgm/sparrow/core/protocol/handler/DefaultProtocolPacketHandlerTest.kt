package com.cbgm.sparrow.core.protocol.handler

import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DefaultProtocolPacketHandlerTest {
    @Test
    fun matchingHandlerReceivesPacketAndContext() =
        runTest {
            val packet = createPacket()
            val context = createContext()
            val matchingHandler = RecordingHandler(canHandle = true)
            val nonMatchingHandler = RecordingHandler(canHandle = false)
            val handler =
                DefaultProtocolPacketHandler(
                    handlers =
                        listOf(
                            nonMatchingHandler,
                            matchingHandler
                        )
                )

            val result = handler.handle(context = context, packet = packet)

            assertTrue(result.isSuccess)
            assertFalse(nonMatchingHandler.handled)
            assertTrue(matchingHandler.handled)
            assertSame(context, matchingHandler.context)
            assertSame(packet, matchingHandler.packet)
        }

    @Test
    fun onlyFirstMatchingHandlerIsInvoked() =
        runTest {
            val first = RecordingHandler(canHandle = true)
            val second = RecordingHandler(canHandle = true)
            val handler = DefaultProtocolPacketHandler(handlers = listOf(first, second))

            val result =
                handler.handle(
                    context = createContext(),
                    packet = createPacket()
                )

            assertTrue(result.isSuccess)
            assertTrue(first.handled)
            assertFalse(second.handled)
        }

    @Test
    fun missingHandlerReturnsFailure() =
        runTest {
            val handler =
                DefaultProtocolPacketHandler(
                    handlers = listOf(RecordingHandler(canHandle = false))
                )

            val result =
                handler.handle(
                    context = createContext(),
                    packet = createPacket()
                )

            assertTrue(result.isFailure)
            assertEquals(
                expected = "No handler registered for ChatMessagePacket",
                actual = result.exceptionOrNull()?.message
            )
        }

    @Test
    fun handlerFailureIsPropagated() =
        runTest {
            val expectedError = IllegalStateException("handler failed")
            val handler =
                DefaultProtocolPacketHandler(
                    handlers =
                        listOf(
                            RecordingHandler(
                                canHandle = true,
                                result = Result.failure(expectedError)
                            )
                        )
                )

            val result =
                handler.handle(
                    context = createContext(),
                    packet = createPacket()
                )

            assertTrue(result.isFailure)
            assertSame(expectedError, result.exceptionOrNull())
        }

    private fun createPacket(): ChatMessagePacket =
        ChatMessagePacket(
            packetId = "packet-1",
            messageId = "message-1",
            sentAtEpochMilliseconds = 1L,
            text = "Hello"
        )

    private fun createContext(): IncomingPacketContext =
        IncomingPacketContext(
            contactId = "contact-1",
            conversationId = "conversation-1",
            encodedTransportPayload = "payload",
            transportMode = "PLAINTEXT",
            receivedAtEpochMilliseconds = 2L
        )

    private class RecordingHandler(
        private val canHandle: Boolean,
        private val result: Result<Unit> = Result.success(Unit)
    ) : TypedProtocolPacketHandler {
        var handled: Boolean = false
        var context: IncomingPacketContext? = null
        var packet: SparrowPacket? = null

        override fun canHandle(packet: SparrowPacket): Boolean = canHandle

        override suspend fun handle(
            context: IncomingPacketContext,
            packet: SparrowPacket
        ): Result<Unit> {
            handled = true
            this.context = context
            this.packet = packet

            return result
        }
    }
}
