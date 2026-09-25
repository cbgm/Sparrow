package com.cbgm.sparrow.feature.chats.presentation.create.mapper

import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupConversationUiState
import com.cbgm.sparrow.feature.contacts.presentation.overview.mapper.filterContactUi
import com.cbgm.sparrow.feature.contacts.presentation.overview.mapper.groupContactsByInitial
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactUi

internal fun List<ContactUi>.toCreateGroupConversationUiState(
    title: String,
    searchQuery: String,
    selectedContactIds: Set<String>,
    isCreating: Boolean,
    errorMessage: String?
): CreateGroupConversationUiState {
    val availableContactIds = mapTo(mutableSetOf(), ContactUi::id)
    return CreateGroupConversationUiState(
        title = title,
        searchQuery = searchQuery,
        contactGroups = filterContactUi(searchQuery).groupContactsByInitial(),
        selectedContactIds = selectedContactIds.intersect(availableContactIds),
        isCreating = isCreating,
        errorMessage = errorMessage
    )
}
