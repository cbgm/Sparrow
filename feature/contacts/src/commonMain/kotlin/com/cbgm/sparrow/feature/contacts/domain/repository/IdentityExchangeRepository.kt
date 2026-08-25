package com.cbgm.sparrow.feature.contacts.domain.repository

interface IdentityExchangeRepository {
    /**
     * Starts only the explicit manual identity exchange.
     *
     * The caller is responsible for cancelling any automatic invitation first.
     */
    suspend fun startManualExchange(contactId: String): Result<Unit>
}
