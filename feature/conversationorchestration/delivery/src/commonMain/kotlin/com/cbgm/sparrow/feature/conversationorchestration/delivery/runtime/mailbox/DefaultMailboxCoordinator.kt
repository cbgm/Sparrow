package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DefaultMailboxCoordinator(
    private val routeProvisioner: MailboxRouteProvisioner,
    private val pendingSynchronizer: MailboxPendingSynchronizer,
    private val arrivalListener: MailboxArrivalListener
) : MailboxCoordinator {
    private val logger = SparrowLog.withTag("DefaultMailboxCoordinator")

    override suspend fun provisionRoutes(): Result<Int> = routeProvisioner.provision()

    override suspend fun synchronizePending(): Result<Int> = pendingSynchronizer.synchronize()

    override suspend fun listen() {
        coroutineScope {
            launch { arrivalListener.listen() }
            launch { provisionNewContacts() }
        }
    }

    /** New contacts get their mailbox within [NEW_CONTACT_CHECK_MILLISECONDS], not only at the next connect. */
    private suspend fun provisionNewContacts() {
        while (currentCoroutineContext().isActive) {
            delay(NEW_CONTACT_CHECK_MILLISECONDS.milliseconds)
            routeProvisioner
                .provisionMissing()
                .onSuccess { provisioned -> if (provisioned > 0) logger.info { "Mailbox routes provisioned for new contacts: $provisioned" } }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.info { "Mailbox routes for new contacts deferred (${error.message})" }
                }
        }
    }

    private companion object {
        const val NEW_CONTACT_CHECK_MILLISECONDS = 15_000L
    }
}
