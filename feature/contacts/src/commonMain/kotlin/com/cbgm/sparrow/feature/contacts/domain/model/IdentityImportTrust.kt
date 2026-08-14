package com.cbgm.sparrow.feature.contacts.domain.model

/**
 * How the imported public identity was authenticated by the user.
 */
enum class IdentityImportTrust {
    /**
     * The identity was copied, shared remotely, or otherwise imported
     * without an independent proof that it belongs to the expected person.
     */
    UNVERIFIED,

    /**
     * The identity QR code was scanned directly from the other person's
     * device during an in-person verification.
     */
    VERIFIED_IN_PERSON
}
