package com.cbgm.sparrow.feature.settings.data.repository

import com.cbgm.sparrow.feature.settings.data.datasource.DeveloperErrorLogStorageDataSource
import com.cbgm.sparrow.feature.settings.data.mapper.toDeveloperError
import com.cbgm.sparrow.feature.settings.domain.model.DeveloperError
import com.cbgm.sparrow.feature.settings.domain.repository.DeveloperErrorLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeveloperErrorLogRepositoryImpl(
    private val dataSource: DeveloperErrorLogStorageDataSource
) : DeveloperErrorLogRepository {
    override fun observeErrors(): Flow<List<DeveloperError>> =
        dataSource.observeErrors().map { errors -> errors.map { error -> error.toDeveloperError() } }

    override suspend fun clearErrors() {
        dataSource.clearErrors()
    }
}
