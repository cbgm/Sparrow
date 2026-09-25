package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.data.database.dao.ApprovedIdentityReconnectionDao
import com.cbgm.sparrow.data.database.entity.ApprovedIdentityReconnectionEntity
import com.cbgm.sparrow.feature.identity.data.datasource.ApprovedIdentityReconnectionDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApprovedIdentityReconnectionRepositoryImplTest {
    @Test
    fun persistsApprovalAcrossObserverSubscriptionsAndAcknowledgesExactIdOnly() = runBlocking {
        val dao = FakeDao()
        val repository = ApprovedIdentityReconnectionRepositoryImpl(ApprovedIdentityReconnectionDataSource(dao))
        assertEquals("old", repository.observeAll().first().single().approvalId)
        assertEquals("old", repository.find("peer").getOrThrow()?.approvalId)
        assertTrue(repository.acknowledgeQueued("peer", "superseded").isFailure)
        assertEquals("old", repository.find("peer").getOrThrow()?.approvalId)
        repository.acknowledgeQueued("peer", "old").getOrThrow()
        assertNull(repository.find("peer").getOrThrow())
    }

    @Test
    fun databaseReadFailureMustNotBeConvertedToMissingConsent() = runBlocking {
        val dao = FakeDao()
        val repository = ApprovedIdentityReconnectionRepositoryImpl(ApprovedIdentityReconnectionDataSource(dao))
        dao.failRead = true
        assertTrue(repository.find("peer").isFailure)
    }

    private class FakeDao : ApprovedIdentityReconnectionDao {
        private val rows = MutableStateFlow(listOf(ApprovedIdentityReconnectionEntity("peer", "old", 1L)))
        var failRead = false

        override fun observeAll(): Flow<List<ApprovedIdentityReconnectionEntity>> = rows

        override suspend fun find(peerId: String): ApprovedIdentityReconnectionEntity? {
            if (failRead) error("DB unavailable")
            return rows.value.firstOrNull { it.peerId == peerId }
        }

        override suspend fun deleteIfCurrent(peerId: String, approvalId: String): Int {
            if (rows.value.none { it.peerId == peerId && it.approvalId == approvalId }) return 0
            rows.value = rows.value.filterNot { it.peerId == peerId && it.approvalId == approvalId }
            return 1
        }
    }
}
