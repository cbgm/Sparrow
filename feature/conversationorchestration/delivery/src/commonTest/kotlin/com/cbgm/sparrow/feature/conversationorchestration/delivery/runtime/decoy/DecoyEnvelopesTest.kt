package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.decoy

import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox.InMemoryMailboxRoutes
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox.RecordingMailboxGateway
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox.mailboxCredential
import com.cbgm.sparrow.feature.transport.domain.gateway.InvitationMailbox
import com.cbgm.sparrow.feature.transport.domain.gateway.MailboxGateway
import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences
import com.cbgm.sparrow.feature.transport.domain.model.ConnectionMode
import com.cbgm.sparrow.feature.transport.domain.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveDecoyCountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DecoyEnvelopesTest {
    private val routes =
        InMemoryMailboxRoutes().apply {
            listOf("recipient", "a", "b", "c", InvitationMailbox.CONTACT_ID).forEach { contactId ->
                local[contactId] = mailboxCredential(contactId = contactId, name = contactId, expiresAt = Long.MAX_VALUE)
            }
        }
    private val count = MutableStateFlow(0)
    private var time = 10_000L
    private val sealed = mutableListOf<String>()

    @Test
    fun withDecoysOffNothingIsPrepared() =
        runTest {
            assertTrue(decoys(this).prepare("recipient").isEmpty())
            assertTrue(sealed.isEmpty())
        }

    @Test
    fun decoysGoToOtherContactsNeverToTheRecipientOrTheInvitationMailbox() =
        runTest {
            count.value = 2

            val prepared = decoys(this).prepare("recipient")

            assertEquals(2, prepared.size)
            assertEquals(2, prepared.map(FederatedEnvelope::envelopeId).toSet().size)
            assertTrue(prepared.all { decoy -> decoy.envelopeId in setOf("decoy-a", "decoy-b", "decoy-c") })
        }

    @Test
    fun aGroupFanOutGetsOneSetOfDecoysNotOnePerMember() =
        runTest {
            count.value = 1
            val decoys = decoys(this)

            assertEquals(1, decoys.prepare("a").size)
            time += 300L
            assertTrue(decoys.prepare("b").isEmpty())
            time += 1_000L
            assertEquals(1, decoys.prepare("c").size)
        }

    @Test
    fun withoutAProtectingCircuitNoDecoysGoOut() =
        runTest {
            count.value = 3

            assertTrue(decoys(this, protecting = false).prepare("recipient").isEmpty())
        }

    @Test
    fun contactsWithoutARouteAreSkipped() =
        runTest {
            count.value = 3
            val noRoute = setOf("a", "b")

            val prepared = decoys(this, unsealable = noRoute).prepare("recipient")

            assertEquals(listOf("decoy-c"), prepared.map(FederatedEnvelope::envelopeId))
        }

    @Test
    fun preparedDecoysAreDeposited() =
        runTest {
            count.value = 2
            val gateway = DepositRecorder()
            val decoys = decoys(this, gateway = gateway)

            decoys.depositAlongside(decoys.prepare("recipient"))
            advanceUntilIdle()

            assertEquals(2, gateway.deposited.size)
        }

    private fun decoys(
        scope: CoroutineScope,
        protecting: Boolean = true,
        unsealable: Set<String> = emptySet(),
        gateway: MailboxGateway = DepositRecorder()
    ) = DecoyEnvelopes(
        observeDecoyCount = ObserveDecoyCountUseCase(DecoyPreferences(count)),
        hasProtectingCircuit = { protecting },
        mailboxRouteRepository = routes,
        sealer =
            { contactId, createdAt ->
                sealed += contactId
                if (contactId in unsealable) {
                    null
                } else {
                    FederatedEnvelope(
                        envelopeId = "decoy-$contactId",
                        senderRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                        recipientDeviceRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                        encryptedPayload = "sealed",
                        createdAtEpochMilliseconds = createdAt,
                        expiresAtEpochMilliseconds = createdAt + 1L
                    )
                }
            },
        mailboxGateway = gateway,
        random = Random(1),
        now = { time },
        scope = scope
    )

    private class DepositRecorder(
        base: MailboxGateway = RecordingMailboxGateway()
    ) : MailboxGateway by base {
        val deposited = mutableListOf<FederatedEnvelope>()

        override suspend fun deliver(envelope: FederatedEnvelope): Result<Unit> {
            deposited += envelope
            return Result.success(Unit)
        }
    }

    private class DecoyPreferences(
        private val count: MutableStateFlow<Int>
    ) : TransportPreferences {
        override fun observeCircuitCount(): Flow<Int> = error("Not used")

        override suspend fun setCircuitCount(circuitCount: Int) = error("Not used")

        override fun observeHopCount(): Flow<Int> = error("Not used")

        override suspend fun setHopCount(hopCount: Int) = error("Not used")

        override fun observeDecoyCount(): Flow<Int> = count

        override suspend fun setDecoyCount(decoyCount: Int) {
            count.value = decoyCount
        }

        override fun observeSendsDirectly(): Flow<Boolean> = error("Not used")

        override suspend fun setSendsDirectly(sendsDirectly: Boolean) = error("Not used")

        override fun observePreferredNode(): Flow<String?> = error("Not used")

        override suspend fun setPreferredNode(nodeId: String?) = error("Not used")

        override fun observeConnectionMode(): Flow<ConnectionMode> = error("Not used")

        override suspend fun setConnectionMode(mode: ConnectionMode) = error("Not used")
    }
}
