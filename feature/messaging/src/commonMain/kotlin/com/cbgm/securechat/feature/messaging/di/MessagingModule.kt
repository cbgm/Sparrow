package com.cbgm.securechat.feature.messaging.di

import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor
import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectTypingRepository
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupTypingRepository
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.securechat.feature.messaging.application.incoming.DefaultIncomingEnvelopeProcessor
import com.cbgm.securechat.feature.messaging.application.incoming.DefaultIncomingRelayRunner
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingEnvelopeProcessor
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingRelayRunner
import com.cbgm.securechat.feature.messaging.application.mailbox.DefaultMailboxCapabilityLifecycle
import com.cbgm.securechat.feature.messaging.application.mailbox.DefaultMailboxCoordinator
import com.cbgm.securechat.feature.messaging.application.mailbox.MailboxCoordinator
import com.cbgm.securechat.feature.messaging.application.mailbox.MailboxRoutePacketHandler
import com.cbgm.securechat.feature.messaging.application.mailbox.MailboxRoutePayloadEncoder
import com.cbgm.securechat.feature.messaging.application.outbox.DefaultOutboxProcessor
import com.cbgm.securechat.feature.messaging.application.outbox.DefaultOutboxRunner
import com.cbgm.securechat.feature.messaging.application.outbox.DefaultOutgoingPacketTransportPolicy
import com.cbgm.securechat.feature.messaging.application.outbox.DefaultOutgoingTransportPayloadFactory
import com.cbgm.securechat.feature.messaging.application.outbox.OutgoingPacketTransportPolicy
import com.cbgm.securechat.feature.messaging.application.outbox.OutgoingTransportPayloadFactory
import com.cbgm.securechat.feature.messaging.application.relay.ContactByRelayIdResolver
import com.cbgm.securechat.feature.messaging.application.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.messaging.application.relay.GroupRelayIdResolver
import com.cbgm.securechat.feature.messaging.application.relay.GroupTransportKeyResolver
import com.cbgm.securechat.feature.messaging.application.relay.IncomingRelayGateway
import com.cbgm.securechat.feature.messaging.data.relay.DefaultContactByRelayIdResolver
import com.cbgm.securechat.feature.messaging.data.relay.DefaultContactRelayIdResolver
import com.cbgm.securechat.feature.messaging.data.relay.DefaultGroupRelayIdResolver
import com.cbgm.securechat.feature.messaging.data.relay.DefaultGroupTransportKeyResolver
import com.cbgm.securechat.feature.messaging.data.relay.WebSocketIncomingRelayGateway
import com.cbgm.securechat.feature.messaging.data.repository.direct.DirectTypingRepositoryImpl
import com.cbgm.securechat.feature.messaging.data.repository.group.GroupTypingRepositoryImpl
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val messagingModule =
    module {
        single<ContactRelayIdResolver> {
            DefaultContactRelayIdResolver(
                getContact = get<GetContactUseCase>(),
                contactRelayIdDao = get(),
                relayIdGenerator = get<RelayIdGenerator>()
            )
        }

        single<ContactByRelayIdResolver> {
            DefaultContactByRelayIdResolver(
                contactRepository = get<ContactRepository>(),
                contactDao = get<ContactDao>(),
                contactRelayIdDao = get(),
                relayIdGenerator = get<RelayIdGenerator>(),
                groupRelayIdResolver = get<GroupRelayIdResolver>()
            )
        }

        single<GroupRelayIdResolver> {
            DefaultGroupRelayIdResolver(
                chatDao = get(),
                groupSecurityDao = get(),
                relayIdGenerator = get<RelayIdGenerator>()
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
                contactRelayIdResolver = get<ContactRelayIdResolver>()
            )
        }

        single<GroupTypingRepository> {
            GroupTypingRepositoryImpl(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                groupRelayIdResolver = get<GroupRelayIdResolver>()
            )
        }

        single<IncomingRelayGateway> {
            WebSocketIncomingRelayGateway(
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
                contactRelayIdResolver = get<ContactRelayIdResolver>(),
                groupRelayIdResolver = get<GroupRelayIdResolver>(),
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
                contactByRelayIdResolver = get<ContactByRelayIdResolver>(),
                localEncryptionKeyPairProvider = get(),
                incomingMessageHandler = get()
            )
        }

        single<IncomingRelayRunner> {
            DefaultIncomingRelayRunner(
                incomingRelayGateway = get<IncomingRelayGateway>(),
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
                contactRelayIdDao = get(),
                localRelayIdProvider = get(),
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
