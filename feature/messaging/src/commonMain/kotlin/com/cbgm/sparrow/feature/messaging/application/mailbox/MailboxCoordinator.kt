package com.cbgm.sparrow.feature.messaging.application.mailbox

interface MailboxCoordinator {
    suspend fun provisionRoutes(): Result<Int>

    suspend fun synchronizePending(): Result<Int>
}
