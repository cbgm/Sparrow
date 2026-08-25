package com.cbgm.sparrow.feature.messaging.runtime.mailbox

import com.cbgm.sparrow.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.core.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultMailboxCapabilityLifecycleTest {
    @Test
    fun failedRevocationIsPersistedAndRetried() =
        runTest {
            val repository = FakeMailboxRouteRepository(credential())
            val gateway = FakeMailboxGateway(failRevocation = true)
            val lifecycle = DefaultMailboxCapabilityLifecycle(repository, gateway)

            assertTrue(lifecycle.revokeForContact(CONTACT_ID).isFailure)
            assertTrue(repository.remoteDeleted)
            assertTrue(repository.credential?.revocationPending == true)

            gateway.failRevocation = false
            assertEquals(1, lifecycle.retryPendingRevocations().getOrThrow())
            assertNull(repository.credential)
            assertEquals(2, gateway.revocationAttempts)
        }

    @Test
    fun successfulRevocationRemovesLocalAndRemoteRoutes() =
        runTest {
            val repository = FakeMailboxRouteRepository(credential())
            val gateway = FakeMailboxGateway(failRevocation = false)
            val lifecycle = DefaultMailboxCapabilityLifecycle(repository, gateway)

            lifecycle.revokeForContact(CONTACT_ID).getOrThrow()

            assertTrue(repository.remoteDeleted)
            assertNull(repository.credential)
            assertEquals(1, gateway.revocationAttempts)
            assertFalse(lifecycle.retryPendingRevocations().isFailure)
        }

    private class FakeMailboxRouteRepository(
        var credential: LocalMailboxCredential?
    ) : MailboxRouteRepository {
        var remoteDeleted = false

        override suspend fun localForContact(contactId: String) = Result.success(credential)

        override suspend fun remoteForRecipientRoutingId(routingId: String) = Result.success<MailboxDeliveryRoute?>(null)

        override suspend fun allLocal() = Result.success(listOfNotNull(credential))

        override suspend fun saveLocal(credential: LocalMailboxCredential): Result<Unit> {
            this.credential = credential
            return Result.success(Unit)
        }

        override suspend fun saveRemote(
            contactId: String,
            route: MailboxDeliveryRoute
        ) = Result.success(Unit)

        override suspend fun markLocalRevocationPending(contactId: String): Result<Unit> {
            credential = credential?.copy(revocationPending = true)
            return Result.success(Unit)
        }

        override suspend fun deleteLocal(contactId: String): Result<Unit> {
            credential = null
            return Result.success(Unit)
        }

        override suspend fun deleteRemote(contactId: String): Result<Unit> {
            remoteDeleted = true
            return Result.success(Unit)
        }

        override suspend fun deleteAllRemote(): Result<Unit> {
            remoteDeleted = true
            return Result.success(Unit)
        }
    }

    private class FakeMailboxGateway(
        var failRevocation: Boolean
    ) : MailboxGateway {
        var revocationAttempts = 0

        override suspend fun create(
            contactId: String,
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            sequence: Long,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun pending(credential: LocalMailboxCredential) = Result.success(emptyList<FederatedEnvelope>())

        override suspend fun acknowledge(
            credential: LocalMailboxCredential,
            envelopeId: String
        ) = Result.success(Unit)

        override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> {
            revocationAttempts += 1
            return if (failRevocation) {
                Result.failure(IllegalStateException("offline"))
            } else {
                Result.success(Unit)
            }
        }
    }

    private companion object {
        const val CONTACT_ID = "contact-1"

        fun credential() =
            LocalMailboxCredential(
                contactId = CONTACT_ID,
                deliveryRoute =
                    MailboxDeliveryRoute(
                        routeId = "route-1",
                        nodeId = "node-a",
                        nodeEndpoint = "http://mailbox",
                        mailboxId = "mailbox-1",
                        sendCapability = "send-capability",
                        sequence = 0,
                        expiresAtEpochMilliseconds = Long.MAX_VALUE,
                        identitySignature = byteArrayOf(1)
                    ),
                accessEndpoint = "http://mailbox",
                retrievalCapability = "retrieval-capability"
            )
    }
}
