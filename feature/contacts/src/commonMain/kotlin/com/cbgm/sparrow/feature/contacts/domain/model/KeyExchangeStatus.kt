package com.cbgm.sparrow.feature.contacts.domain.model

enum class KeyExchangeStatus {
    /**
     * We possess this contact's public keys, but the contact has not
     * confirmed possession of our current public keys.
     *
     * Messages must be sent as plaintext transport packets.
     */
    ONE_WAY,

    /**
     * Both parties possess each other's current public keys.
     *
     * Messages can be end-to-end encrypted.
     */
    MUTUAL
}
