package com.cbgm.sparrow.feature.contacts.domain.model.device

/**
 * A contact returned by the platform address book.
 *
 * This is not a Sparrow contact.
 */
data class DeviceContact(
    /**
     * Stable platform identifier.
     *
     * Android:
     * ContactsContract contact ID.
     *
     * iOS:
     * CNContact identifier.
     */
    val id: String,
    val displayName: String?,
    val phoneNumbers: List<DevicePhoneNumber>
)
