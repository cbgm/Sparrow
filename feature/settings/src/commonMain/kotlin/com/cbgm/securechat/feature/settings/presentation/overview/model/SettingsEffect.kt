package com.cbgm.securechat.feature.settings.presentation.overview.model

sealed interface SettingsEffect {
    data class ShowSnackbar(
        val message: String
    ) : SettingsEffect
}
