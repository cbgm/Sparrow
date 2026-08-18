package com.cbgm.sparrow.feature.chats.presentation.create.mapper

import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.presentation.overview.mapper.filterContacts
import com.cbgm.sparrow.feature.contacts.presentation.overview.mapper.groupContactsByInitial

internal fun List<Contact>.toUiState(
    profilePictures: Map<String, ByteArray?>,
    title: String,
    searchQuery: String,
    selectedContactIds: Set<String>,
    isCreating: Boolean,
    errorMessage: String?
): CreateGroupUiState {
    val availableContactIds = mapTo(mutableSetOf(), Contact::id)
    return CreateGroupUiState(
        title = title,
        searchQuery = searchQuery,
        contactGroups = filterContacts(searchQuery).groupContactsByInitial(),
        profilePictures = profilePictures,
        selectedContactIds = selectedContactIds.intersect(availableContactIds),
        isCreating = isCreating,
        errorMessage = errorMessage
    )
}
