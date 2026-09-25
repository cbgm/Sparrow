package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.feature.identity.data.datasource.LocalIdentitySharingDataSource
import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentitySharingRepository
import kotlinx.coroutines.flow.Flow

internal class LocalIdentitySharingRepositoryImpl(
    private val dataSource: LocalIdentitySharingDataSource
) : LocalIdentitySharingRepository {
    override fun observeSharedWith(peerId: String): Flow<Boolean> = dataSource.observeSharedWith(peerId)

    override suspend fun recordSharedWith(peerId: String) = dataSource.recordSharedWith(peerId)
}
