package com.cbgm.sparrow.presentation.model

import com.cbgm.sparrow.core.protocol.outbox.OutboxRunner
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeRunner
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxCoordinator
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.notification.domain.model.AppVisibilityState

data class ForegroundRuntimeDependencies(
    val appVisibilityState: AppVisibilityState,
    val incomingEnvelopeRunner: IncomingEnvelopeRunner,
    val transportConnectionManager: TransportConnectionManager,
    val outboxRunner: OutboxRunner,
    val mailboxCoordinator: MailboxCoordinator
)
