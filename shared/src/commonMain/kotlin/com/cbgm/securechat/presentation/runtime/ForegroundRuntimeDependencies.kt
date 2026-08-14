package com.cbgm.securechat.presentation.runtime

import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingEnvelopeRunner
import com.cbgm.securechat.feature.messaging.application.mailbox.MailboxCoordinator
import com.cbgm.securechat.feature.transport.connection.TransportConnectionManager
import com.cbgm.securechat.notification.application.AppVisibilityState

data class ForegroundRuntimeDependencies(
    val appVisibilityState: AppVisibilityState,
    val incomingEnvelopeRunner: IncomingEnvelopeRunner,
    val transportConnectionManager: TransportConnectionManager,
    val outboxRunner: OutboxRunner,
    val mailboxCoordinator: MailboxCoordinator
)
