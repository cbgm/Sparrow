package com.cbgm.sparrow.feature.settings.presentation.profile.model

sealed interface ProfileSettingsUiEvent {
    data object BackClicked : ProfileSettingsUiEvent

    data class PictureSelected(
        val bytes: ByteArray
    ) : ProfileSettingsUiEvent

    data object RemovePictureClicked : ProfileSettingsUiEvent
}
