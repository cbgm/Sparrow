package com.cbgm.securechat.feature.messaging.application.incoming

import com.cbgm.securechat.core.protocol.handler.IncomingMessageHandler
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.feature.messaging.application.routing.ContactByRoutingIdResolver
import com.cbgm.securechat.feature.messaging.application.routing.IncomingEnvelopeGateway
import com.cbgm.securechat.feature.messaging.application.routing.IncomingTransportEnvelope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultIncomingEnvelopeRunnerTest {
    @Test
    fun successfulHandlingAcknowledgesEnvelopeAfterHandlerReturns() =
        runTest {
            val events = mutableListOf<String>()
            val envelopeGateway = FakeIncomingEnvelopeGateway(events)
            val incomingHandler =
                RecordingIncomingMessageHandler(
                    events = events
                )
            val runner =
                createRunner(
                    envelopeGateway = envelopeGateway,
                    incomingHandler = incomingHandler
                )

            try {
                runner.start()
                envelopeGateway.emitEnvelope(createEnvelope())

                val acknowledgedEnvelopeId = envelopeGateway.acknowledgedEnvelopeIds.receive()

                assertEquals("envelope-1", acknowledgedEnvelopeId)
                assertEquals(listOf("handle", "acknowledge"), events)
                assertEquals("contact-1", incomingHandler.contactId)
                assertEquals("encoded-payload", incomingHandler.encodedTransportPayload)
                assertContentEquals(PUBLIC_KEY, incomingHandler.localEncryptionPublicKey)
                assertContentEquals(PRIVATE_KEY, incomingHandler.localEncryptionPrivateKey)
            } finally {
                runner.stop()
            }
        }

    @Test
    fun failedHandlingDoesNotAcknowledgeEnvelope() =
        runTest {
            val envelopeGateway = FakeIncomingEnvelopeGateway()
            val handlerCalled = CompletableDeferred<Unit>()
            val incomingHandler =
                RecordingIncomingMessageHandler(
                    result = {
                        handlerCalled.complete(Unit)
                        throw IllegalStateException("message handling failed")
                    }
                )
            val runner =
                createRunner(
                    envelopeGateway = envelopeGateway,
                    incomingHandler = incomingHandler
                )

            try {
                runner.start()
                envelopeGateway.emitEnvelope(createEnvelope())
                handlerCalled.await()

                val acknowledgement =
                    withTimeoutOrNull(SHORT_TIMEOUT_MILLISECONDS) {
                        envelopeGateway.acknowledgedEnvelopeIds.receive()
                    }

                assertNull(acknowledgement)
                assertEquals(1, incomingHandler.callCount)
            } finally {
                runner.stop()
            }
        }

    @Test
    fun unknownSenderIsIgnoredWithoutLoadingKeysOrAcknowledging() =
        runTest {
            val envelopeGateway = FakeIncomingEnvelopeGateway()
            val resolverCalled = CompletableDeferred<Unit>()
            val keyPairProvider = RecordingKeyPairProvider()
            val incomingHandler = RecordingIncomingMessageHandler()
            val runner =
                createRunner(
                    envelopeGateway = envelopeGateway,
                    contactResolver =
                        object : ContactByRoutingIdResolver {
                            override suspend fun resolveContactId(routingId: String): Result<String?> {
                                resolverCalled.complete(Unit)
                                return Result.success(null)
                            }
                        },
                    keyPairProvider = keyPairProvider,
                    incomingHandler = incomingHandler
                )

            try {
                runner.start()
                envelopeGateway.emitEnvelope(createEnvelope())
                resolverCalled.await()

                val acknowledgement =
                    withTimeoutOrNull(SHORT_TIMEOUT_MILLISECONDS) {
                        envelopeGateway.acknowledgedEnvelopeIds.receive()
                    }

                assertNull(acknowledgement)
                assertEquals(0, keyPairProvider.callCount)
                assertEquals(0, incomingHandler.callCount)
            } finally {
                runner.stop()
            }
        }

    private fun createRunner(
        envelopeGateway: FakeIncomingEnvelopeGateway,
        contactResolver: ContactByRoutingIdResolver =
            object : ContactByRoutingIdResolver {
                override suspend fun resolveContactId(routingId: String): Result<String?> = Result.success("contact-1")
            },
        keyPairProvider: LocalEncryptionKeyPairProvider =
            RecordingKeyPairProvider(),
        incomingHandler: IncomingMessageHandler
    ): DefaultIncomingEnvelopeRunner =
        DefaultIncomingEnvelopeRunner(
            incomingEnvelopeGateway = envelopeGateway,
            incomingEnvelopeProcessor =
                DefaultIncomingEnvelopeProcessor(
                    contactByRoutingIdResolver = contactResolver,
                    localEncryptionKeyPairProvider = keyPairProvider,
                    incomingMessageHandler = incomingHandler
                )
        )

    private fun createEnvelope(): IncomingTransportEnvelope =
        IncomingTransportEnvelope(
            envelopeId = "envelope-1",
            senderRoutingId = "sender-routing-id",
            encodedTransportPayload = "encoded-payload"
        )

    private class RecordingIncomingMessageHandler(
        private val events: MutableList<String> = mutableListOf(),
        private val result: suspend () -> Unit = {}
    ) : IncomingMessageHandler {
        var callCount: Int = 0
        var contactId: String? = null
        var encodedTransportPayload: String? = null
        var localEncryptionPublicKey: ByteArray? = null
        var localEncryptionPrivateKey: ByteArray? = null

        override suspend fun handle(
            contactId: String,
            encodedTransportPayload: String,
            localEncryptionPublicKey: ByteArray,
            localEncryptionPrivateKey: ByteArray
        ) {
            callCount += 1
            this.contactId = contactId
            this.encodedTransportPayload = encodedTransportPayload
            this.localEncryptionPublicKey = localEncryptionPublicKey.copyOf()
            this.localEncryptionPrivateKey = localEncryptionPrivateKey.copyOf()
            events += "handle"
            result()
        }
    }

    private class RecordingKeyPairProvider : LocalEncryptionKeyPairProvider {
        var callCount: Int = 0

        override suspend fun getEncryptionKeyPair(): Result<LocalEncryptionKeyPair> {
            callCount += 1

            return Result.success(
                LocalEncryptionKeyPair(
                    publicKey = PUBLIC_KEY,
                    privateKey = PRIVATE_KEY
                )
            )
        }
    }

    private class FakeIncomingEnvelopeGateway(
        private val events: MutableList<String> = mutableListOf()
    ) : IncomingEnvelopeGateway {
        private val mutableIncomingEnvelopes = MutableSharedFlow<IncomingTransportEnvelope>()
        override val incomingEnvelopes = mutableIncomingEnvelopes

        val acknowledgedEnvelopeIds = Channel<String>(capacity = Channel.UNLIMITED)

        suspend fun emitEnvelope(envelope: IncomingTransportEnvelope) {
            mutableIncomingEnvelopes.subscriptionCount.first { subscriberCount ->
                subscriberCount > 0
            }
            mutableIncomingEnvelopes.emit(envelope)
        }

        override suspend fun acknowledge(envelopeId: String): Result<Unit> {
            events += "acknowledge"
            acknowledgedEnvelopeIds.send(envelopeId)

            return Result.success(Unit)
        }
    }

    private companion object {
        val PUBLIC_KEY = byteArrayOf(1, 2, 3)
        val PRIVATE_KEY = byteArrayOf(4, 5, 6)

        const val SHORT_TIMEOUT_MILLISECONDS = 150L
    }
}
