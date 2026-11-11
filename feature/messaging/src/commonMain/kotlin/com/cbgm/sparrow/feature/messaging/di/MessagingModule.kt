package com.cbgm.sparrow.feature.messaging.di

import com.cbgm.sparrow.feature.messaging.domain.usecase.AcknowledgeMessagingFailureUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingFailureEventsUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingIndicatorsUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingTransportResultsUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.SendEncodedTransportUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.SendMessagingIndicatorUseCase
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxRoutePayloadEncoder
import com.cbgm.sparrow.feature.messaging.runtime.outbox.DefaultOutboxRunner
import com.cbgm.sparrow.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.protocol.outbox.OutboxRunner
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.transport.OutgoingWireSender
import org.koin.dsl.module

val messagingModule =
    module {
        single { SendEncodedTransportUseCase(outgoingWireSender = get<OutgoingWireSender>()) }
        single { ObserveMessagingTransportResultsUseCase(protocolOutbox = get<ProtocolOutbox>()) }
        single { ObserveMessagingFailureEventsUseCase(outbox = get<ProtocolOutbox>()) }
        single { AcknowledgeMessagingFailureUseCase(outbox = get<ProtocolOutbox>()) }
        single<OutboxRunner> {
            DefaultOutboxRunner(
                protocolOutbox = get<ProtocolOutbox>(),
                outboxProcessor = get<OutboxProcessor>()
            )
        }
        single { MailboxRoutePayloadEncoder() }
        single { SendMessagingIndicatorUseCase(gateway = get()) }
        single { ObserveMessagingIndicatorsUseCase(gateway = get()) }
    }
