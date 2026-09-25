package com.cbgm.sparrow.feature.messaging.di

import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.core.protocol.outbox.OutboxRunner
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireSender
import com.cbgm.sparrow.feature.messaging.domain.usecase.AcknowledgeMessagingFailureUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingFailureEventsUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingIndicatorsUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingTransportResultsUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.SendEncodedTransportUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.SendMessagingIndicatorUseCase
import com.cbgm.sparrow.feature.messaging.runtime.incoming.DefaultIncomingEnvelopeRunner
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeGateway
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeRunner
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxRoutePayloadEncoder
import com.cbgm.sparrow.feature.messaging.runtime.outbox.DefaultOutboxRunner
import org.koin.dsl.module

val messagingModule =
    module {
        single { SendEncodedTransportUseCase(outgoingWireSender = get<OutgoingWireSender>()) }
        single { ObserveMessagingTransportResultsUseCase(protocolOutbox = get<ProtocolOutbox>()) }
        single { ObserveMessagingFailureEventsUseCase(outbox = get<ProtocolOutbox>()) }
        single { AcknowledgeMessagingFailureUseCase(outbox = get<ProtocolOutbox>()) }
        single<IncomingEnvelopeRunner> {
            DefaultIncomingEnvelopeRunner(
                incomingEnvelopeGateway = get<IncomingEnvelopeGateway>(),
                incomingEnvelopeProcessor = get<IncomingEnvelopeProcessor>()
            )
        }
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
