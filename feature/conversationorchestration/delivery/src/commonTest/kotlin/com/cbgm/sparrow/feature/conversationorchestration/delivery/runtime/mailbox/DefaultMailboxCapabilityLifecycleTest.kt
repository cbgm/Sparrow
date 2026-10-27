package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultMailboxCapabilityLifecycleTest {
    @Test
    fun failedRevocationIsPersistedAndRetriedWithoutFailingTheCaller() =
        runTest {
            val repository = FakeMailboxRouteRepository(credential())
            val gateway = FakeMailboxGateway(failRevocation = true)
            val lifecycle = DefaultMailboxCapabilityLifecycle(repository, gateway)

            assertTrue(lifecycle.revokeForContact(CONTACT_ID).isSuccess)
            assertTrue(repository.remoteDeleted)
            assertTrue(repository.credential?.revocationPending == true)

            assertEquals(0, lifecycle.retryPendingRevocations().getOrThrow())
            assertTrue(repository.credential?.revocationPending == true)

            gateway.failRevocation = false
            assertEquals(1, lifecycle.retryPendingRevocations().getOrThrow())
            assertNull(repository.credential)
            assertEquals(3, gateway.revocationAttempts)
        }

    @Test
    fun unreachableNodeDefersRevocation() =
        runTest {
            val repository = FakeMailboxRouteRepository(credential())
            val gateway = FakeMailboxGateway(failRevocation = true, failure = ConnectTimeoutException())
            val lifecycle = DefaultMailboxCapabilityLifecycle(repository, gateway)

            assertTrue(lifecycle.revokeForContact(CONTACT_ID).isSuccess)
            assertTrue(repository.credential?.revocationPending == true)
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

    @Test
    fun revokingAContactAlsoRevokesItsRetiringMailbox() =
        runTest {
            val old = credential().let { it.copy(deliveryRoute = it.deliveryRoute.copy(routeId = "old-route")) }
            val repository = FakeMailboxRouteRepository(credential(), mutableListOf(old))
            val gateway = FakeMailboxGateway(failRevocation = true)
            val lifecycle = DefaultMailboxCapabilityLifecycle(repository, gateway)

            assertTrue(lifecycle.revokeForContact(CONTACT_ID).isSuccess)
            // A failed revocation stays pending, so the retiring mailbox is no longer read.
            assertTrue(repository.retiring.single().revocationPending)

            gateway.failRevocation = false
            assertEquals(2, lifecycle.retryPendingRevocations().getOrThrow())
            assertTrue(repository.retiring.isEmpty())
            assertNull(repository.credential)
        }

    private class FakeMailboxRouteRepository(
        var credential: LocalMailboxCredential?,
        val retiring: MutableList<LocalMailboxCredential> = mutableListOf()
    ) : MailboxRouteRepository {
        var remoteDeleted = false

        override suspend fun allRetiring() = Result.success(retiring.toList())

        override suspend fun retireLocal(retiring: LocalMailboxCredential, replacement: LocalMailboxCredential) =
            error("Not used")

        override suspend fun saveRetiring(credential: LocalMailboxCredential) = error("Not used")

        override suspend fun markRetiringRevocationPending(contactId: String): Result<Unit> {
            retiring.replaceAll { stored ->
                if (stored.contactId == contactId) stored.copy(revocationPending = true) else stored
            }
            return Result.success(Unit)
        }

        override suspend fun deleteRetiring(routeId: String): Result<Unit> {
            retiring.removeAll { stored -> stored.deliveryRoute.routeId == routeId }
            return Result.success(Unit)
        }

        override suspend fun localForContact(contactId: String) = Result.success(credential)

        override suspend fun remoteForRecipientRoutingId(routingId: String) =
            Result.success<MailboxDeliveryRoute?>(null)

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

    // Named like the platform exception that a connect timeout raises.
    private class ConnectTimeoutException : Exception("connect timeout")

    private class FakeMailboxGateway(
        var failRevocation: Boolean,
        private val failure: Exception = IllegalStateException("offline")
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

        override suspend fun pending(credential: LocalMailboxCredential) =
            Result.success(emptyList<FederatedEnvelope>())

        override suspend fun acknowledge(
            credential: LocalMailboxCredential,
            envelopeId: String
        ) = Result.success(Unit)

        override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> {
            revocationAttempts += 1
            return if (failRevocation) {
                Result.failure(failure)
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
