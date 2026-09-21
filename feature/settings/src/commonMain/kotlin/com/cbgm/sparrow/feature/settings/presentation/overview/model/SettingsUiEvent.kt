package com.cbgm.sparrow.feature.settings.presentation.overview.model

import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode

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

    data class SemanticSearchEnabledChanged(
        val enabled: Boolean
    ) : SettingsUiEvent

    data class MessageSafetyEnabledChanged(
        val enabled: Boolean
    ) : SettingsUiEvent

    data class VoiceTranscriptionEnabledChanged(
        val enabled: Boolean
    ) : SettingsUiEvent

    data object PrivacyPolicyClicked : SettingsUiEvent

    data object DataDisclaimerClicked : SettingsUiEvent

    data object LicensesClicked : SettingsUiEvent

    data object DeveloperMenuClicked : SettingsUiEvent

    data object BlockedContactsClicked : SettingsUiEvent

    data object AutoReplyClicked : SettingsUiEvent

    data object ControlPlanesClicked : SettingsUiEvent

    data object AttachmentStorageClicked : SettingsUiEvent

    data object VersionRowTapped : SettingsUiEvent
}
