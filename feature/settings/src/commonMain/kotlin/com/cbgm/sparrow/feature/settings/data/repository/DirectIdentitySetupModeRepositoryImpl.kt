package com.cbgm.sparrow.feature.settings.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.settings.data.datasource.SettingsStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

class DirectIdentitySetupModeRepositoryImpl(
    private val settingsStorage: SettingsStorage
) : DirectIdentitySetupModeRepository {
    private val mode = MutableStateFlow(DirectIdentitySetupMode.AUTOMATIC_INVITATION)

    override fun observeMode(): Flow<DirectIdentitySetupMode> =
        mode
            .onStart {
                mode.value = loadMode()
            }.distinctUntilChanged()

    override suspend fun getMode(): Result<DirectIdentitySetupMode> =
        safeSuspendCall {
            val storedMode = loadMode()
            mode.value = storedMode
            storedMode
        }

    override suspend fun setMode(mode: DirectIdentitySetupMode): Result<Unit> =
        safeSuspendCall {
            settingsStorage.setDirectIdentitySetupMode(mode.name)
            this.mode.value = mode
        }

    private suspend fun loadMode(): DirectIdentitySetupMode =
        settingsStorage
            .getDirectIdentitySetupMode()
            ?.let { storedValue ->
                DirectIdentitySetupMode.entries.firstOrNull { candidate ->
                    candidate.name == storedValue
                }
            } ?: DirectIdentitySetupMode.AUTOMATIC_INVITATION
}
