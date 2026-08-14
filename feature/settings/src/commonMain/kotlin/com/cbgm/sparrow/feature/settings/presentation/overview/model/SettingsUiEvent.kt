package com.cbgm.sparrow.feature.settings.presentation.overview.model

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.locale.AppLanguage

sealed interface SettingsUiEvent {
    data object LanguagePickerOpened : SettingsUiEvent

    data object LanguagePickerDismissed : SettingsUiEvent

    data class LanguageSelected(
        val language: AppLanguage
    ) : SettingsUiEvent

    data class DirectIdentitySetupModeChanged(
        val mode: DirectIdentitySetupMode
    ) : SettingsUiEvent

    data class BlockUnknownContactInvitesChanged(
        val enabled: Boolean
    ) : SettingsUiEvent

    data object PrivacyPolicyClicked : SettingsUiEvent

    data object DataDisclaimerClicked : SettingsUiEvent

    data object LicensesClicked : SettingsUiEvent

    data object DeveloperMenuClicked : SettingsUiEvent

    data object BlockedContactsClicked : SettingsUiEvent

    data object ControlPlanesClicked : SettingsUiEvent

    data object VersionRowTapped : SettingsUiEvent
}
