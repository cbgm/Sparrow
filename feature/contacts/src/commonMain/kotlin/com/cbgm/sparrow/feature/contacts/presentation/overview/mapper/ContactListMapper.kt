package com.cbgm.sparrow.feature.contacts.presentation.overview.mapper

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactGroupEntity
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactUi
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

fun List<ContactUi>.groupContactsByInitial(): List<ContactGroupEntity> =
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

internal fun List<Contact>.toContactsUiState(query: String): ContactsUiState {
    val filteredContacts = filterContacts(query)
    return when {
        isEmpty() -> ContactsUiState.Empty(searchQuery = query)
        filteredContacts.isEmpty() ->
            ContactsUiState.Content(
                groups = emptyList(),
                searchQuery = query
            )
        else ->
            ContactsUiState.Content(
                groups = filteredContacts.map(Contact::toContactUi).groupContactsByInitial(),
                searchQuery = query
            )
    }
}

/** Converts domain contacts to presentation data before grouping. */
fun List<Contact>.toContactGroups(): List<ContactGroupEntity> =
    map(Contact::toContactUi).groupContactsByInitial()

/** Search used by presentation-only contact lists such as the group picker. */
fun List<ContactUi>.filterContactUi(query: String): List<ContactUi> {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return this
    val phoneQuery = trimmedQuery.filter(Char::isDigit)
    return filter { contact ->
        contact.displayName?.contains(trimmedQuery, ignoreCase = true) == true ||
            (
                phoneQuery.isNotEmpty() && contact.phoneNumbers.any { number ->
                    number.filter(Char::isDigit).contains(phoneQuery)
                }
            )
    }
}
