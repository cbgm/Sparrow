package com.cbgm.securechat.feature.messaging.application.mailbox

interface MailboxCoordinator {
    suspend fun provisionRoutes(): Result<Int>

    suspend fun synchronizePending(): Result<Int>
}
