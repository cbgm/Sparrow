package com.cbgm.sparrow.feature.contacts.domain.repository

interface IdentityExchangeRepository {
    /**
     * Starts the configured direct-contact setup when the contact is not already mutual.
     *
     * Automatic mode uses the signed invitation handshake. Manual mode sends the local
     * identity only after the remote identity was explicitly imported or scanned.
     */
    suspend fun ensureStarted(contactId: String): Result<Unit>

    /**
     * Starts only the explicit manual identity exchange.
     *
     * This is used after a QR or identity-text import and must never create an automatic
     * contact invitation or an invitation-acceptance dialog.
     */
    suspend fun startManualExchange(contactId: String): Result<Unit>
}
