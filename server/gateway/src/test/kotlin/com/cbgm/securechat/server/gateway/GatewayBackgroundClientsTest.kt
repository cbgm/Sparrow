package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.RelayEnvelope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
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
    fun queuedPushFallbackDoesNotBlockCaller() =
        runTest {
            val storeStarted = CompletableDeferred<Unit>()
            val releaseStore = CompletableDeferred<Unit>()
            val markedStored = CompletableDeferred<Unit>()
            val dispatcher =
                GatewayPushDispatcher(
                    pushClient =
                        object : LegacyPushClient {
                            override suspend fun store(envelope: RelayEnvelope): Boolean {
                                storeStarted.complete(Unit)
                                releaseStore.await()
                                return true
                            }

                            override suspend fun pending(recipientId: String): List<RelayEnvelope> =
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

    private fun testEnvelope(): RelayEnvelope =
        RelayEnvelope(
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
