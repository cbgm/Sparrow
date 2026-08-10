package com.cbgm.securechat.presentation.runtime

import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingRelayRunner
import com.cbgm.securechat.feature.messaging.application.mailbox.MailboxCoordinator
import com.cbgm.securechat.feature.transport.connection.RelayConnectionManager
import com.cbgm.securechat.notification.application.AppVisibilityState

data class ForegroundRuntimeDependencies(
    val appVisibilityState: AppVisibilityState,
    val incomingRelayRunner: IncomingRelayRunner,
    val relayConnectionManager: RelayConnectionManager,
    val outboxRunner: OutboxRunner,
    val mailboxCoordinator: MailboxCoordinator
)
