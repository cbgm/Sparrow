package com.cbgm.securechat.feature.settings.data.repository

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository
import com.cbgm.securechat.feature.settings.data.datasource.SettingsStorage
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

    override suspend fun getMode(): DirectIdentitySetupMode {
        val storedMode = loadMode()
        mode.value = storedMode
        return storedMode
    }

    override suspend fun setMode(mode: DirectIdentitySetupMode) {
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
