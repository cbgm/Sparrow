package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.TransportEnvelope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GatewayBackgroundClientsTest {
    @Test
    fun presenceRegistrationMustBeConfirmedBeforeGatewayAcceptsRoute() =
        runTest {
            val rejectedPresence =
                object : PresenceClient {
                    override suspend fun register(registration: ClientRouteRegistration): Boolean = false

                    override suspend fun remove(
                        routingId: String,
                        connectionId: String
                    ) = Unit
                }

            assertFalse(
                synchronizePresenceRegistration(
                    presence = rejectedPresence,
                    registration = testRegistration()
                )
            )
        }

    @Test
    fun presenceRegistrationExceptionIsNotReportedAsSuccess() =
        runTest {
            val failingPresence =
                object : PresenceClient {
                    override suspend fun register(registration: ClientRouteRegistration): Boolean =
                        error("presence unavailable")

                    override suspend fun remove(
                        routingId: String,
                        connectionId: String
                    ) = Unit
                }

            assertFalse(
                synchronizePresenceRegistration(
                    presence = failingPresence,
                    registration = testRegistration()
                )
            )
        }

    @Test
    fun pendingEnvelopesAreLoadedForCanonicalAndAliasRoutingIds() =
        runTest {
            val requestedRoutingIds = mutableListOf<String>()
            val client =
                object : LegacyPushClient {
                    override suspend fun store(envelope: TransportEnvelope): Boolean = true

                    override suspend fun pending(recipientId: String): List<TransportEnvelope> {
                        requestedRoutingIds += recipientId
                        return when (recipientId) {
                            "canonical" ->
                                listOf(testEnvelope().copy(envelopeId = "canonical-envelope"))

                            "scphone1_alias" ->
                                listOf(testEnvelope().copy(envelopeId = "bootstrap-envelope"))
                            else -> emptyList()
                        }
                    }

                    override suspend fun acknowledge(
                        recipientId: String,
                        envelopeId: String
                    ) = Unit
                }

            val pending =
                client.pendingForRoutingIds(
                    setOf("canonical", "scphone1_alias")
                )

            assertEquals(
                setOf("canonical", "scphone1_alias"),
                requestedRoutingIds.toSet()
            )
            assertEquals(2, pending.size)
            assertEquals(
                setOf("canonical-envelope", "bootstrap-envelope"),
                pending.map { envelope -> envelope.envelopeId }.toSet()
            )
        }

    @Test
    fun acknowledgementIsAppliedToCanonicalAndAliasRoutingIds() =
        runTest {
            val acknowledgements = mutableListOf<Pair<String, String>>()
            val client =
                object : LegacyPushClient {
                    override suspend fun store(envelope: TransportEnvelope): Boolean = true

                    override suspend fun pending(recipientId: String): List<TransportEnvelope> = emptyList()

                    override suspend fun acknowledge(
                        recipientId: String,
                        envelopeId: String
                    ) {
                        acknowledgements += recipientId to envelopeId
                    }
                }

            client.acknowledgeForRoutingIds(
                routingIds = setOf("canonical", "scphone1_alias"),
                envelopeId = "envelope-1"
            )

            assertEquals(
                setOf(
                    "canonical" to "envelope-1",
                    "scphone1_alias" to "envelope-1"
                ),
                acknowledgements.toSet()
            )
        }

    @Test
    fun queuedPushFallbackDoesNotBlockCaller() =
        runTest {
            val storeStarted = CompletableDeferred<Unit>()
            val releaseStore = CompletableDeferred<Unit>()
            val markedStored = CompletableDeferred<Unit>()
            val dispatcher =
                GatewayPushDispatcher(
                    pushClient =
                        object : LegacyPushClient {
                            override suspend fun store(envelope: TransportEnvelope): Boolean {
                                storeStarted.complete(Unit)
                                releaseStore.await()
                                return true
                            }

                            override suspend fun pending(recipientId: String): List<TransportEnvelope> =
                                emptyList()

                            override suspend fun acknowledge(
                                recipientId: String,
                                envelopeId: String
                            ) = Unit
                        },
                    markFederationStored = {
                        markedStored.complete(Unit)
                    }
                )

            try {
                dispatcher.scheduleFallback(
                    envelope = testEnvelope(),
                    federationEnvelopeId = "envelope-1"
                )
                withTimeout(TEST_TIMEOUT_MILLISECONDS) {
                    storeStarted.await()
                }
                assertFalse(markedStored.isCompleted)
                releaseStore.complete(Unit)
                withTimeout(TEST_TIMEOUT_MILLISECONDS) {
                    markedStored.await()
                }
            } finally {
                releaseStore.complete(Unit)
                dispatcher.close()
            }
        }

    private fun testRegistration(): ClientRouteRegistration =
        ClientRouteRegistration(
            route =
                ClientRoute(
                    routingId = "routing-id",
                    nodeId = "node-id",
                    connectionId = "connection-id",
                    generation = 1L,
                    expiresAtEpochMilliseconds = 2_000L,
                    clientSignature = byteArrayOf(1)
                ),
            clientSigningPublicKey = byteArrayOf(1)
        )

    private fun testEnvelope(): TransportEnvelope =
        TransportEnvelope(
            envelopeId = "envelope-1",
            senderId = "sender",
            recipientId = "recipient",
            payload = "payload",
            createdAtEpochMilliseconds = 1_000L
        )

    private companion object {
        const val TEST_TIMEOUT_MILLISECONDS = 1_000L
    }
}
