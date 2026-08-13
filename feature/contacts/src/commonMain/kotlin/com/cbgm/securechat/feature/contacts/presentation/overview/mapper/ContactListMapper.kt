package com.cbgm.securechat.feature.contacts.presentation.overview.mapper

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.presentation.overview.model.ContactGroupEntity

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
