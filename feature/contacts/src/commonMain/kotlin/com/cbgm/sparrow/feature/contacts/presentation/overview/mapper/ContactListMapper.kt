package com.cbgm.sparrow.feature.contacts.presentation.overview.mapper

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactGroupEntity
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiState

fun List<Contact>.filterContacts(query: String): List<Contact> {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return this

    val normalizedPhoneQuery = trimmedQuery.filter(Char::isDigit)

    return filter { contact ->
        val matchesName =
            contact.displayName?.contains(
                other = trimmedQuery,
                ignoreCase = true
            ) == true

        val matchesPhone =
            normalizedPhoneQuery.isNotEmpty() &&
                contact.phoneNumbers.any { phoneNumber ->
                    phoneNumber.value
                        .filter(Char::isDigit)
                        .contains(normalizedPhoneQuery)
                }

        matchesName || matchesPhone
    }
}

fun List<Contact>.groupContactsByInitial(): List<ContactGroupEntity> =
    sortedBy { contact ->
        contact.displayName.orEmpty().lowercase()
    }.groupBy { contact ->
        contact.displayName
            ?.trim()
            ?.firstOrNull()
            ?.uppercaseChar()
            ?.takeIf(Char::isLetter)
            ?.toString()
            ?: "#"
    }.map { (title, contacts) ->
        ContactGroupEntity(
            title = title,
            contacts = contacts
        )
    }

internal fun List<Contact>.toUiState(
    query: String,
    profilePictures: Map<String, ByteArray?>
): ContactsUiState {
    val filteredContacts = filterContacts(query)
    return when {
        isEmpty() -> ContactsUiState.Empty(searchQuery = query)
        filteredContacts.isEmpty() ->
            ContactsUiState.Content(
                groups = emptyList(),
                profilePictures = profilePictures,
                searchQuery = query
            )
        else ->
            ContactsUiState.Content(
                groups = filteredContacts.groupContactsByInitial(),
                profilePictures = profilePictures,
                searchQuery = query
            )
    }
}
