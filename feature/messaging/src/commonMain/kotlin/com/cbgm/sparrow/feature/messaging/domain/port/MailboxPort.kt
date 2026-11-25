package com.cbgm.sparrow.feature.messaging.domain.port

interface MailboxPort {
    suspend fun provisionRoutes(): Result<Int>

    suspend fun synchronizePending(): Result<Int>

    /** Receives envelopes as they arrive in the mailboxes, until cancelled. */
    suspend fun listen()
}
