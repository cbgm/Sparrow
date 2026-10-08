package com.cbgm.sparrow.feature.applock.data.repository

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.applock.data.datasource.AppLockSettingsDataSource
import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow

class AppLockRepositoryImpl(
    private val dataSource: AppLockSettingsDataSource
) : AppLockRepository {
    private val logger = SparrowLog.withTag("AppLockSettings")

    override fun observeEnabled(): Flow<Boolean> = dataSource.observeEnabled()

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> =
        safeSuspendCall {
            dataSource.setEnabled(enabled)
        }.onFailure { error ->
            logger.error(error) { "Could not update app-lock setting" }
        }
}
