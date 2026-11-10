package com.cbgm.sparrow.presentation.model

import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendUnsentMessagesUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityKeysUsableUseCase
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeRunner
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxCoordinator
import com.cbgm.sparrow.feature.notification.domain.model.AppVisibilityState
import com.cbgm.sparrow.feature.transport.background.ConnectionKeepAlive
import com.cbgm.sparrow.feature.transport.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveWaitingForRouteUseCase
import com.cbgm.sparrow.protocol.outbox.OutboxRunner

data class ForegroundRuntimeDependencies(
    val appVisibilityState: AppVisibilityState,
    val incomingEnvelopeRunner: IncomingEnvelopeRunner,
    val transportConnectionManager: TransportConnectionManager,
    val circuitRunner: CircuitRunner,
    val outboxRunner: OutboxRunner,
    val mailboxCoordinator: MailboxCoordinator,
    val observeIdentityKeysUsable: ObserveIdentityKeysUsableUseCase,
    val observeWaitingForRoute: ObserveWaitingForRouteUseCase,
    val sendUnsentMessages: SendUnsentMessagesUseCase,
    val connectionKeepAlive: ConnectionKeepAlive
)
