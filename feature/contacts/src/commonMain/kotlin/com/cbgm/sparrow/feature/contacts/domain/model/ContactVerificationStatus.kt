package com.cbgm.sparrow.feature.contacts.domain.model

/**
 * Trust state of an imported contact identity.
 */
enum class ContactVerificationStatus {
    /**
     * Keys were imported, but the user has not independently
     * verified that they belong to the expected person.
     */
    UNVERIFIED,

    /**
     * The user verified the key fingerprint or exchanged the
     * identity through a trusted in-person process.
     */
    VERIFIED
}
