package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.data.datasource.DirectIdentitySetupModeLocalDataSource
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

internal class DirectIdentitySetupModeRepositoryImpl(
    private val localDataSource: DirectIdentitySetupModeLocalDataSource
) : DirectIdentitySetupModeRepository {
    private val mode = MutableStateFlow(DirectIdentitySetupMode.AUTOMATIC_INVITATION)

    override fun observeMode(): Flow<DirectIdentitySetupMode> =
        mode
            .onStart {
                mode.value = localDataSource.get()
            }.distinctUntilChanged()

    override suspend fun getMode(): Result<DirectIdentitySetupMode> =
        safeSuspendCall {
            val storedMode = localDataSource.get()
            mode.value = storedMode
            storedMode
        }

    override suspend fun setMode(mode: DirectIdentitySetupMode): Result<Unit> =
        safeSuspendCall {
            localDataSource.set(mode)
            this.mode.value = mode
        }

    override suspend fun resetMode(): Result<Unit> =
        safeSuspendCall {
            localDataSource.clear()
            mode.value = DirectIdentitySetupMode.AUTOMATIC_INVITATION
        }
}
