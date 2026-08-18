package com.cbgm.sparrow.feature.chats.presentation.details.model

sealed interface GroupAvatarUiEvent {
    data class AvatarSelected(
        val bytes: ByteArray
    ) : GroupAvatarUiEvent

    data object RemoveAvatarClicked : GroupAvatarUiEvent
}
