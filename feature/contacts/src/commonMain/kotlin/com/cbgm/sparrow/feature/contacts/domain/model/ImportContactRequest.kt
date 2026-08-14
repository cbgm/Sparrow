package com.cbgm.sparrow.feature.contacts.domain.model

data class ImportContactRequest(
    /**
     * Existing contact to update.
     *
     * null:
     * resolve by signing key or phone number and create a contact when
     * no match exists.
     *
     * non-null:
     * replace/update the Sparrow identity of exactly this contact.
     */
    val contactId: String? = null,
    val displayName: String?,
    val phoneNumber: String?,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val identityImportTrust: IdentityImportTrust = IdentityImportTrust.UNVERIFIED
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is ImportContactRequest) {
            return false
        }

        return contactId == other.contactId &&
            displayName == other.displayName &&
            phoneNumber == other.phoneNumber &&
            identityImportTrust == other.identityImportTrust &&
            encryptionPublicKey.contentEquals(
                other.encryptionPublicKey
            ) &&
            signingPublicKey.contentEquals(
                other.signingPublicKey
            )
    }

    override fun hashCode(): Int {
        var result = contactId?.hashCode() ?: 0

        result = 31 * result + (displayName?.hashCode() ?: 0)

        result = 31 * result + (phoneNumber?.hashCode() ?: 0)

        result = 31 * result + identityImportTrust.hashCode()

        result = 31 * result + encryptionPublicKey.contentHashCode()

        result = 31 * result + signingPublicKey.contentHashCode()

        return result
    }
}
