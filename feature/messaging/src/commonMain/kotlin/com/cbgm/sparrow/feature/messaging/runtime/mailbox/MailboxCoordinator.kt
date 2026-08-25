package com.cbgm.sparrow.feature.messaging.runtime.mailbox

interface MailboxCoordinator {
    suspend fun provisionRoutes(): Result<Int>

    suspend fun synchronizePending(): Result<Int>
}
