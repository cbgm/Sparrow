package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxCoordinator

class DefaultMailboxCoordinator(
    private val routeProvisioner: MailboxRouteProvisioner,
    private val pendingSynchronizer: MailboxPendingSynchronizer,
    private val arrivalListener: MailboxArrivalListener
) : MailboxCoordinator {
    override suspend fun provisionRoutes(): Result<Int> = routeProvisioner.provision()

    override suspend fun synchronizePending(): Result<Int> = pendingSynchronizer.synchronize()

    override suspend fun listen() = arrivalListener.listen()
}
