package com.cbgm.sparrow.feature.applock.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    fun observeEnabled(): Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean): Result<Unit>
}
