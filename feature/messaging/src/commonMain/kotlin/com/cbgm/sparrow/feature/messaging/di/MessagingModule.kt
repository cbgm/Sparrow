package com.cbgm.sparrow.feature.messaging.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.core.protocol.outbox.OutboxRunner
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireSender
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectTypingRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupTypingRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.messaging.application.incoming.DefaultIncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.messaging.application.incoming.DefaultIncomingEnvelopeRunner
import com.cbgm.sparrow.feature.messaging.application.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.messaging.application.incoming.IncomingEnvelopeRunner
import com.cbgm.sparrow.feature.messaging.application.mailbox.DefaultMailboxCapabilityLifecycle
import com.cbgm.sparrow.feature.messaging.application.mailbox.DefaultMailboxCoordinator
import com.cbgm.sparrow.feature.messaging.application.mailbox.MailboxCoordinator
import com.cbgm.sparrow.feature.messaging.application.mailbox.MailboxRoutePacketHandler
import com.cbgm.sparrow.feature.messaging.application.mailbox.MailboxRoutePayloadEncoder
import com.cbgm.sparrow.feature.messaging.application.outbox.DefaultOutboxProcessor
import com.cbgm.sparrow.feature.messaging.application.outbox.DefaultOutboxRunner
import com.cbgm.sparrow.feature.messaging.application.outbox.DefaultOutgoingPacketTransportPolicy
import com.cbgm.sparrow.feature.messaging.application.outbox.DefaultOutgoingTransportPayloadFactory
import com.cbgm.sparrow.feature.messaging.application.outbox.OutgoingPacketTransportPolicy
import com.cbgm.sparrow.feature.messaging.application.outbox.OutgoingTransportPayloadFactory
import com.cbgm.sparrow.feature.messaging.application.routing.ContactByRoutingIdResolver
import com.cbgm.sparrow.feature.messaging.application.routing.ContactRoutingIdResolver
import com.cbgm.sparrow.feature.messaging.application.routing.GroupRoutingIdResolver
import com.cbgm.sparrow.feature.messaging.application.routing.GroupTransportKeyResolver
import com.cbgm.sparrow.feature.messaging.application.routing.IncomingEnvelopeGateway
import com.cbgm.sparrow.feature.messaging.data.repository.direct.DirectTypingRepositoryImpl
import com.cbgm.sparrow.feature.messaging.data.repository.group.GroupTypingRepositoryImpl
import com.cbgm.sparrow.feature.messaging.data.routing.DefaultContactByRoutingIdResolver
import com.cbgm.sparrow.feature.messaging.data.routing.DefaultContactRoutingIdResolver
import com.cbgm.sparrow.feature.messaging.data.routing.DefaultGroupRoutingIdResolver
import com.cbgm.sparrow.feature.messaging.data.routing.DefaultGroupTransportKeyResolver
import com.cbgm.sparrow.feature.messaging.data.routing.WebSocketIncomingEnvelopeGateway
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val messagingModule =
    module {
        single<ContactRoutingIdResolver> {
            DefaultContactRoutingIdResolver(
                getContact = get<GetContactUseCase>(),
                contactRoutingIdDao = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }

        single<ContactByRoutingIdResolver> {
            DefaultContactByRoutingIdResolver(
                contactRepository = get<ContactRepository>(),
                contactDao = get<ContactDao>(),
                contactRoutingIdDao = get(),
                routingIdGenerator = get<RoutingIdGenerator>(),
                groupRoutingIdResolver = get<GroupRoutingIdResolver>()
            )
        }

        single<GroupRoutingIdResolver> {
            DefaultGroupRoutingIdResolver(
                chatDao = get(),
                groupSecurityDao = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }

        single<GroupTransportKeyResolver> {
            DefaultGroupTransportKeyResolver(
                chatDao = get(),
                groupSecurityDao = get()
            )
        }

        single<DirectTypingRepository> {
            DirectTypingRepositoryImpl(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                contactRoutingIdResolver = get<ContactRoutingIdResolver>()
            )
        }

        single<GroupTypingRepository> {
            GroupTypingRepositoryImpl(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                groupRoutingIdResolver = get<GroupRoutingIdResolver>()
            )
        }

        single<IncomingEnvelopeGateway> {
            WebSocketIncomingEnvelopeGateway(
                webSocketTransportClient = get<WebSocketTransportClient>()
            )
        }

        single<OutgoingPacketTransportPolicy> {
            DefaultOutgoingPacketTransportPolicy()
        }

        single<OutgoingTransportPayloadFactory> {
            DefaultOutgoingTransportPayloadFactory(
                transportMessageCipher = get(),
                packetTransportPolicy = get<OutgoingPacketTransportPolicy>(),
                groupTransportKeyResolver = get<GroupTransportKeyResolver>()
            )
        }

        single<OutboxProcessor> {
            DefaultOutboxProcessor(
                protocolOutbox = get<ProtocolOutbox>(),
                getContact = get<GetContactUseCase>(),
                transportPayloadFactory = get<OutgoingTransportPayloadFactory>(),
                transportPayloadCodec = get(),
                packetCodec = get(),
                contactRoutingIdResolver = get<ContactRoutingIdResolver>(),
                groupRoutingIdResolver = get<GroupRoutingIdResolver>(),
                outgoingWireSender = get<OutgoingWireSender>(),
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
                contactByRoutingIdResolver = get<ContactByRoutingIdResolver>(),
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

        singleOf(::MailboxRoutePacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        single<MailboxCoordinator> {
            DefaultMailboxCoordinator(
                contactDao = get(),
                contactRoutingIdDao = get(),
                localRoutingIdProvider = get(),
                nodeEndpointResolver = get(),
                mailboxGateway = get(),
                mailboxRouteRepository = get(),
                mailboxCapabilityLifecycle = get(),
                contactBlocklistRepository = get(),
                signingKeyPairProvider = get(),
                signatureCrypto = get(),
                payloadEncoder = get(),
                protocolOutbox = get(),
                incomingEnvelopeProcessor = get()
            )
        }
    }
