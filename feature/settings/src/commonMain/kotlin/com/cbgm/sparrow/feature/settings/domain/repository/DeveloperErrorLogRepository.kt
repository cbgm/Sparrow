package com.cbgm.sparrow.feature.settings.domain.repository

import com.cbgm.sparrow.feature.settings.domain.model.DeveloperError
import kotlinx.coroutines.flow.Flow

interface DeveloperErrorLogRepository {
    fun observeErrors(): Flow<List<DeveloperError>>

    suspend fun clearErrors()
}
