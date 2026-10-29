package com.cbgm.sparrow.feature.applock.data.repository

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.applock.data.datasource.AppLockSettingsDataSource
import com.cbgm.sparrow.feature.applock.domain.model.AppLockSettings
import com.cbgm.sparrow.feature.applock.domain.model.AppLockTimeout
import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AppLockRepositoryImpl(
    private val dataSource: AppLockSettingsDataSource
) : AppLockRepository {
    private val logger = SparrowLog.withTag("AppLockSettings")

    override fun observeEnabled(): Flow<Boolean> = dataSource.observeEnabled()

    override fun observeSettings(): Flow<AppLockSettings> =
        combine(dataSource.observeEnabled(), dataSource.observeTimeout()) { enabled, timeout ->
            AppLockSettings(
                enabled = enabled,
                timeout = AppLockTimeout.entries.firstOrNull { it.name == timeout } ?: AppLockTimeout.DEFAULT
            )
        }

    override suspend fun setTimeout(timeout: AppLockTimeout): Result<Unit> =
        safeSuspendCall {
            dataSource.setTimeout(timeout.name)
        }.onFailure { error ->
            logger.error(error) { "Could not update app-lock timeout" }
        }

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> =
        safeSuspendCall {
            dataSource.setEnabled(enabled)
        }.onFailure { error ->
            logger.error(error) { "Could not update app-lock setting" }
        }
}
