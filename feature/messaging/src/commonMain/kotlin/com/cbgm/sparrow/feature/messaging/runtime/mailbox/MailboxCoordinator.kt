package com.cbgm.sparrow.feature.messaging.runtime.mailbox

interface MailboxCoordinator {
    suspend fun provisionRoutes(): Result<Int>

    suspend fun synchronizePending(): Result<Int>

    /** Receives envelopes as they arrive in the mailboxes, until cancelled. */
    suspend fun listen()
}
