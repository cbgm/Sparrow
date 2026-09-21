package com.cbgm.sparrow.feature.identity.data.repository

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
        val repository = PendingRemoteIdentityChangeRepositoryImpl(PendingRemoteIdentityChangeDataSource(dao))
        repository.stage(candidate("new", 100L)).getOrThrow()
        val saved = repository.observeAll().first().single()
        assertEquals("existing", saved.peerId)
        assertEquals("new", saved.invitationId)
        assertTrue(saved.proposedSigningPublicKey.contentEquals(ByteArray(32) { 2 }))
    }

    @Test
    fun oldReplayedInvitationDoesNotReplaceNewerCandidate() = runBlocking {
        val dao = FakeDao()
        val repository = PendingRemoteIdentityChangeRepositoryImpl(PendingRemoteIdentityChangeDataSource(dao))
        repository.stage(candidate("new", 200L)).getOrThrow()
        repository.stage(candidate("old", 100L)).getOrThrow()
        assertEquals("new", repository.observeAll().first().single().invitationId)
    }

    @Test
    fun discardCannotDeleteDifferentInvitationForSameContact() = runBlocking {
        val dao = FakeDao()
        val repository = PendingRemoteIdentityChangeRepositoryImpl(PendingRemoteIdentityChangeDataSource(dao))
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

        override suspend fun deleteIfInvitationMatches(peerId: String, invitationId: String): Int {
            val oldSize = state.value.size
            state.value = state.value.filterNot { it.peerId == peerId && it.invitationId == invitationId }
            return oldSize - state.value.size
        }
    }
}
