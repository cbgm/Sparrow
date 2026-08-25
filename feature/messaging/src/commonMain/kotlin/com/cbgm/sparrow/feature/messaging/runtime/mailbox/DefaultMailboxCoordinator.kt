package com.cbgm.sparrow.feature.messaging.runtime.mailbox

class DefaultMailboxCoordinator(
    private val routeProvisioner: MailboxRouteProvisioner,
    private val pendingSynchronizer: MailboxPendingSynchronizer
) : MailboxCoordinator {
    override suspend fun provisionRoutes(): Result<Int> = routeProvisioner.provision()

    override suspend fun synchronizePending(): Result<Int> = pendingSynchronizer.synchronize()
}
