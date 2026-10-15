package com.cbgm.sparrow.feature.conversationorchestration.delivery.di

import com.cbgm.sparrow.feature.conversationorchestration.api.observer.ConversationIndicatorObserver
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.datasource.DirectDeliveryDataSource
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.datasource.GroupRecipientDeliveryDataSource
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.datasource.MessageConversationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.datasource.WebSocketIncomingEnvelopeGateway
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.repository.DirectMessageDeliveryRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.repository.GroupRecipientDeliveryRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.repository.MessageConversationRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository.DirectMessageDeliveryRepository
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository.GroupRecipientDeliveryRepository
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository.MessageConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.usecase.SendConversationIndicatorUseCase
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.workflow.event.ApplyDeliveryEventWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.workflow.transport.RecordPreparedTransportWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.delivery.OutboxDeliveryStateRouter
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.indicator.TransportConversationIndicatorObserver
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.indicator.WebSocketMessagingIndicatorGateway
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox.DefaultMailboxCapabilityLifecycle
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox.DefaultMailboxCoordinator
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox.MailboxCredentialFactory
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox.MailboxPendingSynchronizer
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox.MailboxRoutePacketHandler
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox.MailboxRouteProvisioner
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.outbox.OutgoingPacketSender
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.outbox.OutgoingPacketTransportPolicy
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.outbox.OutgoingRecipientRoutingResolver
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.outbox.OutgoingTransportPayloadFactory
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.routing.GroupRoutingResolver
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.routing.GroupTransportKeyResolver
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.messaging.domain.usecase.SendEncodedTransportUseCase
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeGateway
import com.cbgm.sparrow.feature.messaging.runtime.indicator.MessagingIndicatorGateway
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxCoordinator
import com.cbgm.sparrow.feature.messaging.runtime.outbox.DefaultOutboxProcessor
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.sparrow.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.sparrow.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationDeliveryModule =
    module {
        registerDeliveryState()
        registerRouting()
        registerOutbox()
        registerMailbox()
        registerIndicators()
    }

private fun Module.registerDeliveryState() {
    singleOf(::DirectDeliveryDataSource)
    singleOf(::GroupRecipientDeliveryDataSource)
    singleOf(::MessageConversationDataSource)
    singleOf(::DirectMessageDeliveryRepositoryImpl) { bind<DirectMessageDeliveryRepository>() }
    singleOf(::GroupRecipientDeliveryRepositoryImpl) { bind<GroupRecipientDeliveryRepository>() }
    singleOf(::MessageConversationRepositoryImpl) { bind<MessageConversationRepository>() }
    singleOf(::ApplyDeliveryEventWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RecordPreparedTransportWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::OutboxDeliveryStateRouter) { bind<OutboxDeliveryStateListener>() }
}

private fun Module.registerRouting() {
    single {
        GroupRoutingResolver(
            messageConversationRepository = get(),
            getGroupRoutingMembers = get(),
            routingIdGenerator = get<RoutingIdGenerator>()
        )
    }
    single { GroupTransportKeyResolver(messageConversationRepository = get(), resolveMemberEncryptionKey = get()) }
}

private fun Module.registerOutbox() {
    singleOf(::OutgoingPacketTransportPolicy)
    single {
        OutgoingTransportPayloadFactory(
            transportMessageCipher = get(),
            packetTransportPolicy = get(),
            groupTransportKeyResolver = get()
        )
    }
    single {
        OutgoingRecipientRoutingResolver(
            resolveContactRoutingId = get(),
            resolveContactBootstrapRoutingId = get(),
            resolveContactInvitationRoutingId = get(),
            groupRoutingResolver = get()
        )
    }
    single {
        OutgoingPacketSender(
            getContact = get(),
            transportPayloadFactory = get(),
            transportPayloadCodec = get(),
            packetCodec = get(),
            recipientRoutingResolver = get(),
            sendEncodedTransport = get<SendEncodedTransportUseCase>(),
            deliveryStateListener = get(),
            observePendingRemoteIdentityChanges = get()
        )
    }
    single<OutboxProcessor> {
        val sender = get<OutgoingPacketSender>()
        DefaultOutboxProcessor(
            protocolOutbox = get<ProtocolOutbox>(),
            send = { item -> sender.send(item) },
            deliveryStateListener = get()
        )
    }
    single<IncomingEnvelopeGateway> {
        WebSocketIncomingEnvelopeGateway(webSocketTransportClient = get<WebSocketTransportClient>())
    }
}

private fun Module.registerMailbox() {
    single<MailboxCapabilityLifecycle> {
        DefaultMailboxCapabilityLifecycle(repository = get(), gateway = get())
    }
    singleOf(::MailboxCredentialFactory)
    singleOf(::MailboxPendingSynchronizer)
    singleOf(::MailboxRouteProvisioner)
    single<MailboxCoordinator> {
        DefaultMailboxCoordinator(routeProvisioner = get(), pendingSynchronizer = get())
    }
    singleOf(::MailboxRoutePacketHandler) { bind<TypedProtocolPacketHandler>() }
}

private fun Module.registerIndicators() {
    single<MessagingIndicatorGateway> {
        WebSocketMessagingIndicatorGateway(client = get<WebSocketTransportClient>())
    }
    singleOf(::SendConversationIndicatorUseCase)
    singleOf(::TransportConversationIndicatorObserver) { bind<ConversationIndicatorObserver>() }
}
