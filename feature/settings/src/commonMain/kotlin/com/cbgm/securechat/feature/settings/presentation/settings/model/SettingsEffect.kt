package com.cbgm.securechat.feature.settings.presentation.settings.model

sealed interface SettingsEffect {
    data class ShowSnackbar(
        val message: String
    ) : SettingsEffect
}
