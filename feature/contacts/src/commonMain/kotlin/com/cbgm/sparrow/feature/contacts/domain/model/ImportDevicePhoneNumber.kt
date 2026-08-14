package com.cbgm.sparrow.feature.contacts.domain.model

/**
 * Phone number imported from the current device's address book.
 *
 * This is input data for importing or synchronizing a device contact.
 * The repository generates the persistent phone-number ID.
 */
data class ImportDevicePhoneNumber(
    val value: String,
    val type: ContactPhoneNumberType,
    val label: String?
)
