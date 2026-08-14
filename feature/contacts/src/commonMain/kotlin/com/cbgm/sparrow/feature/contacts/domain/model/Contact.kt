package com.cbgm.sparrow.feature.contacts.domain.model

/**
 * A person known to Sparrow.
 *
 * The contact may come from:
 *
 * - the device address book
 * - an ordinary shared contact
 * - a Sparrow identity import
 *
 * Sparrow keys may be attached later.
 */
data class Contact(
    val id: String,
    val displayName: String?,
    val phoneNumbers: List<ContactPhoneNumber>,
    val preferredPhoneNumberId: String?,
    /**
     * Identifier of the linked contact in the device address book.
     *
     * Kept when the device contact disappears so Sparrow can
     * display a warning and potentially relink it later.
     */
    val deviceContactId: String?,
    /**
     * Current state of the relationship to the device contact.
     */
    val deviceContactLinkStatus: DeviceContactLinkStatus,
    /**
     * Another person's optional Sparrow identity.
     *
     * Null means this contact currently has no Sparrow keys.
     */
    val sparrowIdentity: SparrowIdentity?,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
) {
    val preferredPhoneNumber: ContactPhoneNumber?
        get() {
            return phoneNumbers.firstOrNull { phoneNumber ->
                phoneNumber.id == preferredPhoneNumberId
            } ?: phoneNumbers.firstOrNull()
        }
}
