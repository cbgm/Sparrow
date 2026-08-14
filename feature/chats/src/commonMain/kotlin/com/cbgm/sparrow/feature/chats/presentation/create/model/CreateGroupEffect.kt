package com.cbgm.sparrow.feature.chats.presentation.create.model

sealed interface CreateGroupEffect {
    data object BackRequested : CreateGroupEffect

    data class GroupCreated(
        val conversationId: String
    ) : CreateGroupEffect
}
