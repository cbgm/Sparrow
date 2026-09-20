package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import kotlinx.coroutines.flow.Flow

interface DirectIdentitySetupModeRepository {
    fun observeMode(): Flow<DirectIdentitySetupMode>

    suspend fun getMode(): DirectIdentitySetupMode

    suspend fun setMode(mode: DirectIdentitySetupMode)
}
