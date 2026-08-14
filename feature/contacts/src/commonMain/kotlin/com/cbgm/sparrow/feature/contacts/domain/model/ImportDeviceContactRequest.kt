package com.cbgm.sparrow.feature.contacts.domain.model

/**
 * Contact information imported from the current device's
 * address book.
 *
 * Sparrow keys are intentionally absent. They may be attached
 * later when the person shares a Sparrow identity.
 */
data class ImportDeviceContactRequest(
    /**
     * Stable identifier supplied by the operating system.
     */
    val deviceContactId: String,
    val displayName: String?,
    /**
     * Every usable phone number exposed by the device contact.
     */
    val phoneNumbers: List<ImportDevicePhoneNumber>
)
