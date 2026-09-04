package com.cbgm.sparrow.feature.chats.presentation.create.model

import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactGroupEntity

data class CreateGroupConversationUiState(
    val title: String = "",
    val searchQuery: String = "",
    val contactGroups: List<ContactGroupEntity> = emptyList(),
    val profilePictures: Map<String, ByteArray?> = emptyMap(),
    val selectedContactIds: Set<String> = emptySet(),
    val isCreating: Boolean = false,
    val errorMessage: String? = null
) {
    val canCreate: Boolean
        get() = title.isNotBlank() && selectedContactIds.isNotEmpty() && !isCreating
}
