package com.cbgm.sparrow.feature.contacts.domain.model

/**
 * Describes the relationship between a Sparrow contact and
 * the contact stored on the current device.
 */
enum class DeviceContactLinkStatus {
    /**
     * This Sparrow contact has never been linked to a contact
     * from the device address book.
     */
    NOT_LINKED,

    /**
     * The linked device contact currently exists.
     */
    LINKED,

    /**
     * A device contact was linked previously, but it can no longer
     * be found.
     *
     * Sparrow keeps the contact, keys, verification state,
     * conversations, and message history.
     */
    MISSING
}
