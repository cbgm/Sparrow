package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import kotlinx.coroutines.flow.Flow

interface DirectIdentitySetupModeRepository {
    fun observeMode(): Flow<DirectIdentitySetupMode>

    suspend fun getMode(): Result<DirectIdentitySetupMode>

    suspend fun setMode(mode: DirectIdentitySetupMode): Result<Unit>

    /** Back to the default, automatic invitation, as when the app was installed. */
    suspend fun resetMode(): Result<Unit>
}
