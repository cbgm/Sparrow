package com.cbgm.sparrow.feature.identity.domain.model

enum class KeyExchangeStatus {
    /**
     * We possess this contact's public keys, but the contact has not
     * confirmed possession of our current public keys.
     *
     * Direct message content must wait for mutual authorization; never send it as plaintext.
     */
    ONE_WAY,

    /**
     * Both parties possess each other's current public keys.
     *
     * Messages can be end-to-end encrypted.
     */
    MUTUAL
}
