package com.cbgm.sparrow.feature.messaging.runtime.incoming

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

class DefaultIncomingEnvelopeRunnerTest {
    @Test
    fun processedEnvelopeIsAcknowledged() =
        runTest {
            val gateway = FakeIncomingEnvelopeGateway()
            val runner = createRunner(gateway, IncomingEnvelopeProcessingResult.Processed)

            try {
                runner.start()
                gateway.emitEnvelope(createEnvelope())

                assertEquals("envelope-1", gateway.acknowledgedEnvelopeIds.receive())
            } finally {
                runner.stop()
            }
        }

    @Test
    fun rejectedEnvelopeIsAcknowledged() =
        runTest {
            val gateway = FakeIncomingEnvelopeGateway()
            val runner = createRunner(gateway, IncomingEnvelopeProcessingResult.Rejected)

            try {
                runner.start()
                gateway.emitEnvelope(createEnvelope())

                assertEquals("envelope-1", gateway.acknowledgedEnvelopeIds.receive())
            } finally {
                runner.stop()
            }
        }

    @Test
    fun unknownSenderIsNotAcknowledged() =
        runTest {
            val gateway = FakeIncomingEnvelopeGateway()
            val runner = createRunner(gateway, IncomingEnvelopeProcessingResult.UnknownSender)

            try {
                runner.start()
                gateway.emitEnvelope(createEnvelope())

                val acknowledgement =
                    withTimeoutOrNull(SHORT_TIMEOUT_MILLISECONDS.milliseconds) {
                        gateway.acknowledgedEnvelopeIds.receive()
                    }
                assertNull(acknowledgement)
            } finally {
                runner.stop()
            }
        }

    @Test
    fun processorFailureIsNotAcknowledged() =
        runTest {
            val gateway = FakeIncomingEnvelopeGateway()
            val runner =
                DefaultIncomingEnvelopeRunner(
                    incomingEnvelopeGateway = gateway,
                    incomingEnvelopeProcessor =
                        object : IncomingEnvelopeProcessor {
                            override suspend fun process(
                                envelopeId: String,
                                senderRoutingId: String,
                                encodedTransportPayload: String
                            ): Result<IncomingEnvelopeProcessingResult> =
                                Result.failure(IllegalStateException("processing failed"))
                        }
                )

            try {
                runner.start()
                gateway.emitEnvelope(createEnvelope())

                val acknowledgement =
                    withTimeoutOrNull(SHORT_TIMEOUT_MILLISECONDS.milliseconds) {
                        gateway.acknowledgedEnvelopeIds.receive()
                    }
                assertNull(acknowledgement)
            } finally {
                runner.stop()
            }
        }

    private fun createRunner(
        gateway: FakeIncomingEnvelopeGateway,
        result: IncomingEnvelopeProcessingResult
    ): DefaultIncomingEnvelopeRunner =
        DefaultIncomingEnvelopeRunner(
            incomingEnvelopeGateway = gateway,
            incomingEnvelopeProcessor =
                object : IncomingEnvelopeProcessor {
                    override suspend fun process(
                        envelopeId: String,
                        senderRoutingId: String,
                        encodedTransportPayload: String
                    ): Result<IncomingEnvelopeProcessingResult> = Result.success(result)
                }
        )

    private fun createEnvelope(): IncomingTransportEnvelope =
        IncomingTransportEnvelope(
            envelopeId = "envelope-1",
            senderRoutingId = "sender-routing-id",
            encodedTransportPayload = "encoded-payload"
        )

    private class FakeIncomingEnvelopeGateway : IncomingEnvelopeGateway {
        private val mutableIncomingEnvelopes = MutableSharedFlow<IncomingTransportEnvelope>()
        override val incomingEnvelopes = mutableIncomingEnvelopes
        val acknowledgedEnvelopeIds = Channel<String>(Channel.UNLIMITED)

        suspend fun emitEnvelope(envelope: IncomingTransportEnvelope) {
            mutableIncomingEnvelopes.subscriptionCount.first { count -> count > 0 }
            mutableIncomingEnvelopes.emit(envelope)
        }

        override suspend fun acknowledge(envelopeId: String): Result<Unit> {
            acknowledgedEnvelopeIds.send(envelopeId)
            return Result.success(Unit)
        }
    }

    private companion object {
        const val SHORT_TIMEOUT_MILLISECONDS = 150L
    }
}
