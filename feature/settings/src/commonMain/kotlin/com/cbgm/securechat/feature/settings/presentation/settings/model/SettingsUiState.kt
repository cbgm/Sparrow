package com.cbgm.securechat.feature.settings.presentation.settings.model

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.ui.locale.AppLanguage
import com.cbgm.securechat.feature.settings.domain.model.BuildInfo

data class SettingsUiState(
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val directIdentitySetupMode: DirectIdentitySetupMode = DirectIdentitySetupMode.AUTOMATIC_INVITATION,
    val blockUnknownContactInvites: Boolean = false,
    val blockedContactCount: Int = 0,
    val buildInfo: BuildInfo = BuildInfo("1.0.0", 1, "release", null),
    val isDeveloperModeEnabled: Boolean = false,
    val developerModeTapCount: Int = 0,
    val showLanguagePicker: Boolean = false,
    val isClearingLocalData: Boolean = false
)

// Taps required on the version row to reveal developer options — classic
const val DEVELOPER_MODE_TAP_THRESHOLD = 7
