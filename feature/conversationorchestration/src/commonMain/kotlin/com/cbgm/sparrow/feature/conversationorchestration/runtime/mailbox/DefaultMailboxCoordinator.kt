package com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox

import com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox.MailboxPendingSynchronizer
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxCoordinator

class DefaultMailboxCoordinator(
    private val routeProvisioner: MailboxRouteProvisioner,
    private val pendingSynchronizer: MailboxPendingSynchronizer
) : MailboxCoordinator {
    override suspend fun provisionRoutes(): Result<Int> = routeProvisioner.provision()

    override suspend fun synchronizePending(): Result<Int> = pendingSynchronizer.synchronize()
}
