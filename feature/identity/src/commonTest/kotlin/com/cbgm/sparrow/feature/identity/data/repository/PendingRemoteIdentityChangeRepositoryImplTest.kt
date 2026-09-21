package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.PendingRemoteIdentityChangeDao
import com.cbgm.sparrow.data.database.entity.PendingRemoteIdentityChangeEntity
import com.cbgm.sparrow.feature.identity.data.datasource.PendingRemoteIdentityChangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PendingRemoteIdentityChangeRepositoryImplTest {
    @Test
    fun stagesUnsignedContinuityAsCandidateWithoutAnAcceptanceOperation() = runBlocking {
        val dao = FakeDao()
        val repository = PendingRemoteIdentityChangeRepositoryImpl(PendingRemoteIdentityChangeDataSource(dao), NoOpMailboxCapabilityLifecycle)
        repository.stage(candidate("new", 100L)).getOrThrow()
        val saved = repository.observeAll().first().single()
        assertEquals("existing", saved.peerId)
        assertEquals("new", saved.invitationId)
        assertTrue(saved.proposedSigningPublicKey.contentEquals(ByteArray(32) { 2 }))
    }

    @Test
    fun oldReplayedInvitationDoesNotReplaceNewerCandidate() = runBlocking {
        val dao = FakeDao()
        val repository = PendingRemoteIdentityChangeRepositoryImpl(PendingRemoteIdentityChangeDataSource(dao), NoOpMailboxCapabilityLifecycle)
        repository.stage(candidate("new", 200L)).getOrThrow()
        repository.stage(candidate("old", 100L)).getOrThrow()
        assertEquals("new", repository.observeAll().first().single().invitationId)
    }

    @Test
    fun replayDoesNotClearPreviousFingerprintConfirmation() = runBlocking {
        val dao = FakeDao()
        val repository = PendingRemoteIdentityChangeRepositoryImpl(PendingRemoteIdentityChangeDataSource(dao), NoOpMailboxCapabilityLifecycle)
        val now = SystemClock.nowEpochMilliseconds()
        repository.stage(candidate("current", now)).getOrThrow()
        val confirmed = repository.confirmFingerprint("existing", "current", ByteArray(32) { 2 }.toFingerprint())
        assertTrue(confirmed.isSuccess)
        repository.stage(candidate("current", now)).getOrThrow()
        assertTrue(repository.observeAll().first().single().fingerprintConfirmedAtEpochMilliseconds != null)
    }

    @Test
    fun differentOrMalformedFingerprintCannotConfirm() = runBlocking {
        val dao = FakeDao()
        val repository = PendingRemoteIdentityChangeRepositoryImpl(PendingRemoteIdentityChangeDataSource(dao), NoOpMailboxCapabilityLifecycle)
        repository.stage(candidate("current", SystemClock.nowEpochMilliseconds())).getOrThrow()
        assertTrue(repository.confirmFingerprint("existing", "current", "1234").isFailure)
        assertTrue(repository.confirmFingerprint("existing", "current", ByteArray(32) { 3 }.toFingerprint()).isFailure)
        assertTrue(repository.observeAll().first().single().fingerprintConfirmedAtEpochMilliseconds == null)
    }

    @Test
    fun failedMailboxRevocationKeepsConfirmedIdentityChangePending() = runBlocking {
        val dao = FakeDao()
        val failingMailbox = object : MailboxCapabilityLifecycle {
            override suspend fun revokeForContact(contactId: String): Result<Unit> =
                Result.failure(IllegalStateException("Mailbox node is unavailable"))

            override suspend fun revokeAll(): Result<Unit> = Result.success(Unit)

            override suspend fun retryPendingRevocations(): Result<Int> = Result.success(0)
        }
        val repository = PendingRemoteIdentityChangeRepositoryImpl(
            PendingRemoteIdentityChangeDataSource(dao),
            failingMailbox
        )
        repository.stage(candidate("current", SystemClock.nowEpochMilliseconds())).getOrThrow()
        repository.confirmFingerprint(
            "existing",
            "current",
            ByteArray(32) { 2 }.toFingerprint()
        ).getOrThrow()
        assertTrue(repository.approveReplacement("existing", "current").isFailure)
        val pending = repository.observeAll().first().single()
        assertEquals("current", pending.invitationId)
        assertTrue(pending.fingerprintConfirmedAtEpochMilliseconds != null)
    }

    @Test
    fun discardCannotDeleteDifferentInvitationForSameContact() = runBlocking {
        val dao = FakeDao()
        val repository = PendingRemoteIdentityChangeRepositoryImpl(PendingRemoteIdentityChangeDataSource(dao), NoOpMailboxCapabilityLifecycle)
        repository.stage(candidate("new", 200L)).getOrThrow()
        repository.discard("existing", "old").getOrThrow()
        assertEquals("new", repository.observeAll().first().single().invitationId)
        repository.discard("existing", "new").getOrThrow()
        assertTrue(repository.observeAll().first().isEmpty())
    }

    private fun candidate(id: String, receivedAt: Long) = PendingRemoteIdentityChange(
        peerId = "existing",
        sourcePeerId = "bootstrap",
        invitationId = id,
        proposedEncryptionPublicKey = ByteArray(32) { 1 },
        proposedSigningPublicKey = ByteArray(32) { 2 },
        receivedAtEpochMilliseconds = receivedAt,
        expiresAtEpochMilliseconds = receivedAt + 10_000
    )

    private class FakeDao : PendingRemoteIdentityChangeDao {
        private val state = MutableStateFlow<List<PendingRemoteIdentityChangeEntity>>(emptyList())

        override suspend fun upsert(candidate: PendingRemoteIdentityChangeEntity) {
            state.value = listOf(candidate)
        }

        override suspend fun findByPeerId(peerId: String): PendingRemoteIdentityChangeEntity? =
            state.value.firstOrNull { it.peerId == peerId }

        override fun observeAll(): Flow<List<PendingRemoteIdentityChangeEntity>> = state

        override suspend fun confirmFingerprintIfCurrent(
            peerId: String,
            invitationId: String,
            proposedSigningPublicKey: ByteArray,
            proposedEncryptionPublicKey: ByteArray,
            confirmedAt: Long
        ): Int {
            val candidate = state.value.singleOrNull() ?: return 0
            if (candidate.peerId != peerId || candidate.invitationId != invitationId ||
                !candidate.proposedSigningPublicKey.contentEquals(proposedSigningPublicKey) ||
                !candidate.proposedEncryptionPublicKey.contentEquals(proposedEncryptionPublicKey) ||
                candidate.fingerprintConfirmedAtEpochMilliseconds != null ||
                candidate.expiresAtEpochMilliseconds <= confirmedAt
            ) {
                return 0
            }
            state.value = listOf(
                candidate.copy(
                    fingerprintConfirmedAtEpochMilliseconds = confirmedAt,
                    confirmedPreviousEncryptionPublicKey = ByteArray(32) { 8 },
                    confirmedPreviousSigningPublicKey = ByteArray(32) { 9 }
                )
            )
            return 1
        }

        override suspend fun countProcessingOutboxForPeer(peerId: String): Int = 0

        override suspend fun quarantineOldRecipientPackets(peerId: String, now: Long): Int = 0

        override suspend fun failUnconfirmedMessagesWithQuarantinedPackets(peerId: String): Int = 0

        override suspend fun replaceIdentityOnlyIfConfirmationStillMatches(
            peerId: String,
            invitationId: String,
            oldEncryptionPublicKey: ByteArray,
            oldSigningPublicKey: ByteArray,
            proposedEncryptionPublicKey: ByteArray,
            proposedSigningPublicKey: ByteArray,
            now: Long
        ): Int = 0

        override suspend fun invalidatePreviousExchanges(peerId: String, now: Long): Int = 0

        override suspend fun deleteIfInvitationMatches(peerId: String, invitationId: String): Int {
            val oldSize = state.value.size
            state.value = state.value.filterNot { it.peerId == peerId && it.invitationId == invitationId }
            return oldSize - state.value.size
        }
    }
}
