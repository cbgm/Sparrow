package com.cbgm.sparrow.feature.applock.domain.repository

import com.cbgm.sparrow.feature.applock.domain.model.AppLockSettings
import com.cbgm.sparrow.feature.applock.domain.model.AppLockTimeout
import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    fun observeEnabled(): Flow<Boolean>

    fun observeSettings(): Flow<AppLockSettings>

    suspend fun setEnabled(enabled: Boolean): Result<Unit>

    suspend fun setTimeout(timeout: AppLockTimeout): Result<Unit>
}
