package com.cbgm.sparrow.core.security

import kotlinx.coroutines.flow.Flow

enum class DirectIdentitySetupMode {
    AUTOMATIC_INVITATION,
    MANUAL_IDENTITY_SHARING
}

interface DirectIdentitySetupModeRepository {
    fun observeMode(): Flow<DirectIdentitySetupMode>

    suspend fun getMode(): DirectIdentitySetupMode

    suspend fun setMode(mode: DirectIdentitySetupMode)
}
