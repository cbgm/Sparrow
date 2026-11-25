package com.cbgm.sparrow.presentation.model

import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendUnsentMessagesUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityKeysUsableUseCase
import com.cbgm.sparrow.feature.messaging.domain.port.MailboxPort
import com.cbgm.sparrow.feature.notification.domain.model.AppVisibilityState
import com.cbgm.sparrow.feature.transport.domain.port.ConnectionKeepAlivePort
import com.cbgm.sparrow.feature.transport.domain.port.TransportConnectionPort
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveWaitingForRouteUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.RunCircuitsUseCase
import com.cbgm.sparrow.protocol.outbox.OutboxRunner

data class ForegroundRuntimeDependencies(
    val appVisibilityState: AppVisibilityState,
    val transportConnection: TransportConnectionPort,
    val runCircuits: RunCircuitsUseCase,
    val outboxRunner: OutboxRunner,
    val mailboxPort: MailboxPort,
    val observeIdentityKeysUsable: ObserveIdentityKeysUsableUseCase,
    val observeWaitingForRoute: ObserveWaitingForRouteUseCase,
    val sendUnsentMessages: SendUnsentMessagesUseCase,
    val connectionKeepAlive: ConnectionKeepAlivePort
)
