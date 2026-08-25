package com.cbgm.sparrow.feature.messaging.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.core.protocol.outbox.OutboxRunner
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireSender
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectTypingRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupTypingRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.messaging.data.datasource.ContactByRoutingIdDataSource
import com.cbgm.sparrow.feature.messaging.data.datasource.ContactRoutingDataSource
import com.cbgm.sparrow.feature.messaging.data.datasource.ContactRoutingReconciliationDataSource
import com.cbgm.sparrow.feature.messaging.data.datasource.GroupRoutingDataSource
import com.cbgm.sparrow.feature.messaging.data.datasource.GroupTransportKeyDataSource
import com.cbgm.sparrow.feature.messaging.data.datasource.MailboxContactDataSource
import com.cbgm.sparrow.feature.messaging.data.datasource.WebSocketIncomingEnvelopeGateway
import com.cbgm.sparrow.feature.messaging.data.repository.DirectTypingRepositoryImpl
import com.cbgm.sparrow.feature.messaging.data.repository.GroupTypingRepositoryImpl
import com.cbgm.sparrow.feature.messaging.runtime.incoming.DefaultIncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.messaging.runtime.incoming.DefaultIncomingEnvelopeRunner
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeGateway
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeRunner
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.DefaultMailboxCapabilityLifecycle
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.DefaultMailboxCoordinator
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxCoordinator
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxCredentialFactory
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxPendingSynchronizer
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxRoutePacketHandler
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxRoutePayloadEncoder
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxRouteProvisioner
import com.cbgm.sparrow.feature.messaging.runtime.outbox.DefaultOutboxProcessor
import com.cbgm.sparrow.feature.messaging.runtime.outbox.DefaultOutboxRunner
import com.cbgm.sparrow.feature.messaging.runtime.outbox.OutgoingPacketSender
import com.cbgm.sparrow.feature.messaging.runtime.outbox.OutgoingPacketTransportPolicy
import com.cbgm.sparrow.feature.messaging.runtime.outbox.OutgoingRecipientRoutingResolver
import com.cbgm.sparrow.feature.messaging.runtime.outbox.OutgoingTransportPayloadFactory
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val messagingModule =
    module {
        single {
            ContactRoutingDataSource(
                contactDao = get(),
                contactRoutingIdDao = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }
        single {
            GroupRoutingDataSource(
                chatDao = get(),
                groupSecurityDao = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }
        single {
            ContactByRoutingIdDataSource(
                contactDao = get(),
                contactRoutingIdDao = get(),
                routingIdGenerator = get<RoutingIdGenerator>(),
                groupRoutingDataSource = get()
            )
        }
        single {
            ContactRoutingReconciliationDataSource(
                contactDao = get(),
                contactRoutingIdDao = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }
        single {
            GroupTransportKeyDataSource(
                chatDao = get(),
                groupSecurityDao = get()
            )
        }
        single {
            MailboxContactDataSource(
                contactDao = get(),
                contactRoutingIdDao = get()
            )
        }

        single<DirectTypingRepository> {
            DirectTypingRepositoryImpl(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                contactRoutingDataSource = get()
            )
        }
        single<GroupTypingRepository> {
            GroupTypingRepositoryImpl(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                groupRoutingDataSource = get()
            )
        }

        single<IncomingEnvelopeGateway> {
            WebSocketIncomingEnvelopeGateway(
                webSocketTransportClient = get<WebSocketTransportClient>()
            )
        }

        singleOf(::OutgoingPacketTransportPolicy)
        single {
            OutgoingTransportPayloadFactory(
                transportMessageCipher = get(),
                packetTransportPolicy = get(),
                groupTransportKeyDataSource = get()
            )
        }
        single {
            OutgoingRecipientRoutingResolver(
                contactRoutingDataSource = get(),
                groupRoutingDataSource = get()
            )
        }
        single {
            OutgoingPacketSender(
                getContact = get<GetContactUseCase>(),
                transportPayloadFactory = get(),
                transportPayloadCodec = get(),
                packetCodec = get(),
                recipientRoutingResolver = get(),
                outgoingWireSender = get<OutgoingWireSender>(),
                deliveryStateListener = get()
            )
        }
        single<OutboxProcessor> {
            DefaultOutboxProcessor(
                protocolOutbox = get<ProtocolOutbox>(),
                packetSender = get(),
                deliveryStateListener = get()
            )
        }
        single<OutboxRunner> {
            DefaultOutboxRunner(
                protocolOutbox = get<ProtocolOutbox>(),
                outboxProcessor = get<OutboxProcessor>()
            )
        }

        single<IncomingEnvelopeProcessor> {
            DefaultIncomingEnvelopeProcessor(
                contactByRoutingIdDataSource = get(),
                contactRoutingReconciliationDataSource = get(),
                localEncryptionKeyPairProvider = get(),
                incomingMessageHandler = get()
            )
        }
        single<IncomingEnvelopeRunner> {
            DefaultIncomingEnvelopeRunner(
                incomingEnvelopeGateway = get<IncomingEnvelopeGateway>(),
                incomingEnvelopeProcessor = get<IncomingEnvelopeProcessor>()
            )
        }

        singleOf(::MailboxRoutePayloadEncoder)
        single<MailboxCapabilityLifecycle> {
            DefaultMailboxCapabilityLifecycle(
                repository = get(),
                gateway = get()
            )
        }
        singleOf(::MailboxCredentialFactory)
        singleOf(::MailboxRouteProvisioner)
        singleOf(::MailboxPendingSynchronizer)
        single<MailboxCoordinator> {
            DefaultMailboxCoordinator(
                routeProvisioner = get(),
                pendingSynchronizer = get()
            )
        }
        singleOf(::MailboxRoutePacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }
    }
