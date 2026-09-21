package com.cbgm.sparrow.feature.conversationorchestration.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.feature.conversationorchestration.data.datasource.WebSocketIncomingEnvelopeGateway
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.AddConversationMembersUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.DeleteConversationGroupUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.DeletePeerConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.GetConversationGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.GroupVerificationInputsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.LeaveConversationGroupUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ObserveConversationIndicatorUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ObserveConversationQueueAvailabilityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PrepareConversationMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PrepareConversationOpenUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PromoteConversationGroupMemberUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.RemoveConversationGroupMemberUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ResolveIncomingIdentityPeerUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ResolveSigningIdentityContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.SendConversationIndicatorUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.StartRecoveryInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.TransferConversationGroupAdminAndLeaveUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.conversationorchestration.runtime.ContactBlockObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.GroupMembershipPacketObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.IdentityExchangePacketObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.IdentityResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.InvitationResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.MembershipResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.MessagingTransportResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.incoming.DefaultIncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.conversationorchestration.runtime.indicator.WebSocketMessagingIndicatorGateway
import com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox.DefaultMailboxCapabilityLifecycle
import com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox.DefaultMailboxCoordinator
import com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox.MailboxCredentialFactory
import com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox.MailboxPendingSynchronizer
import com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox.MailboxRoutePacketHandler
import com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox.MailboxRouteProvisioner
import com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox.InvitationTransportFailureHandler
import com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox.OutgoingPacketSender
import com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox.OutgoingPacketTransportPolicy
import com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox.OutgoingRecipientRoutingResolver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox.OutgoingTransportPayloadFactory
import com.cbgm.sparrow.feature.conversationorchestration.runtime.routing.GroupRoutingResolver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.routing.GroupTransportKeyResolver
import com.cbgm.sparrow.feature.messaging.domain.usecase.SendEncodedTransportUseCase
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeGateway
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.messaging.runtime.indicator.MessagingIndicatorGateway
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxCoordinator
import com.cbgm.sparrow.feature.messaging.runtime.outbox.DefaultOutboxProcessor
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationOrchestrationModule =
    module {
        factory { RequireDirectChatAuthorizationUseCase(get(), get()) }
        factory { GroupVerificationInputsUseCase(membershipRepository = get(), contacts = get(), getRemoteIdentity = get()) }
        factory { ResolveSigningIdentityContactUseCase(contacts = get(), findIdentityPeerId = get(), getRemoteIdentity = get()) }
        factory {
            ResolveIncomingIdentityPeerUseCase(
                inspectContactPeer = get(),
                findRemoteIdentityPeerId = get(),
                getRemoteIdentity = get()
            )
        }

        singleOf(::GroupMembershipPacketObserver) {
            bind<TypedProtocolPacketHandler>()
        }
        singleOf(::IdentityExchangePacketObserver) {
            bind<TypedProtocolPacketHandler>()
        }

        single {
            ConversationFlowHandler(
                startIdentityExchange = get(),
                getIdentityPeerState = get(),
                localPhoneNumberProvider = get(),
                acceptIdentityExchange = get(),
                declineIdentityExchange = get(),
                receiveIdentityExchange = get(),
                receiveIdentityExchangeAccepted = get(),
                recordRemoteIdentityDecline = get(),
                invitationDeclineProtocol = get(),
                invitationRequestProtocol = get(),
                invitationHandshakeProtocol = get(),
                receiveIdentityReady = get(),
                getIdentityExchangeBinding = get(),
                invalidateIdentityExchange = get(),
                revocationProtocol = get(),
                getIdentityExchangeClosure = get(),
                closeIdentityExchange = get(),
                cancelIdentityExchange = get(),
                startManualIdentityExchange = get(),
                revocationSender = get(),
                mailboxCapabilityLifecycle = get(),
                receiveManualIdentity = get(),
                receiveIdentityAcknowledgement = get(),
                reassignIdentityExchangePeer = get(),
                getIdentityPeerDisplayName = get(),
                resolveIncomingIdentityPeer = get(),
                stagePendingRemoteIdentityChange = get(),
                applyIdentityPeerMerge = get(),
                updateIncomingIdentityPeerMetadata = get(),
                phoneNumberNormalizer = get(),
                startGroupMembership = get(),
                inspectIncomingGroupMembership = get(),
                receiveIncomingGroupMembership = get(),
                discardSupersededMemberships = get(),
                getMembershipHandshake = get(),
                acceptGroupMembership = get(),
                declineGroupMembership = get(),
                receiveGroupMembershipReceipt = get(),
                receiveGroupMembershipDecline = get(),
                receiveGroupMembershipJoinRequest = get(),
                receiveGroupReadyAcknowledgement = get(),
                getContact = get(),
                getRemoteIdentity = get(),
                authorizeIncomingGroupWelcome = get(),
                openIncomingGroupWelcome = get(),
                persistIncomingGroupWelcome = get(),
                resolveIncomingPeerContacts = get(),
                completeIncomingGroupWelcome = get(),
                sendGroupReadyAcknowledgement = get(),
                authorizeIncomingGroupDeletion = get(),
                authorizeIncomingGroupRemoval = get(),
                authorizeIncomingGroupActivation = get(),
                applyIncomingGroupActivation = get(),
                resolveSigningIdentityContact = get(),
                completeIncomingGroupRemoval = get(),
                completeIncomingGroupDeletion = get(),
                receiveGroupActivationAcknowledgement = get(),
                receiveGroupLeaveRequest = get(),
                confirmGroupMembershipIdentity = get(),
                clearMembershipHandshake = get(),
                markMembershipRemoved = get(),
                getGroupLeaveRequirementUseCase = get(),
                getGroupCurrentEpochUseCase = get(),
                promoteGroupMemberUseCase = get(),
                removeGroupMemberUseCase = get(),
                transferGroupAdminAndLeaveUseCase = get(),
                leaveGroupUseCase = get(),
                deleteGroupMembershipUseCase = get(),
                stageRemoteIdentity = get(),
                acceptRemoteIdentityHandshake = get(),
                establishMutualIdentity = get(),
                ensureRemoteSigningIdentity = get(),
                applyRemoteProfilePictureMetadata = get(),
                recordInvitation = get(),
                shouldRecordPendingInvitation = get(),
                handleInvitationResponse = get(),
                markInvitationTransportFailed = get(),
                invalidatePendingInvitation = get(),
                blockContact = get(),
                sendContactVerificationReceipt = get(),
                blocklistRepository = get(),
                identitySetupModeRepository = get(),
                conversationPort = get()
            )
        }
        singleOf(::InvitationResultObserver)
        singleOf(::MembershipResultObserver)
        single { MessagingTransportResultObserver(observeFailures = get(), acknowledgeFailure = get(), invitationFailureHandler = get()) }
        single<MessagingIndicatorGateway> {
            WebSocketMessagingIndicatorGateway(client = get<WebSocketTransportClient>())
        }
        singleOf(::SendConversationIndicatorUseCase)
        singleOf(::ObserveConversationIndicatorUseCase)
        singleOf(::IdentityResultObserver)
        singleOf(::ContactBlockObserver)
        singleOf(::ObserveConversationQueueAvailabilityUseCase)
        singleOf(::PrepareConversationMessageUseCase)
        singleOf(::PrepareConversationOpenUseCase)
        factory { StartRecoveryInvitationUseCase(get()) }
        singleOf(::AddConversationMembersUseCase)
        singleOf(::GetConversationGroupLeaveRequirementUseCase)
        singleOf(::PromoteConversationGroupMemberUseCase)
        singleOf(::RemoveConversationGroupMemberUseCase)
        singleOf(::TransferConversationGroupAdminAndLeaveUseCase)
        singleOf(::LeaveConversationGroupUseCase)
        singleOf(::DeleteConversationGroupUseCase)
        singleOf(::DeletePeerConversationUseCase)

        // Cross-feature message preparation and recipient resolution live in orchestration.
        // Generic wire send, runners, and transport endpoints remain owned by Messaging.
        single {
            GroupRoutingResolver(
                conversationPort = get(),
                getGroupRoutingMembers = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }
        single { GroupTransportKeyResolver(conversationPort = get(), resolveMemberEncryptionKey = get()) }

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
                deliveryStateListener = get()
            )
        }
        single { InvitationTransportFailureHandler(packetCodec = get(), invitationOutboxDeliveryHandler = get()) }
        single<OutboxProcessor> {
            val sender = get<OutgoingPacketSender>()
            DefaultOutboxProcessor(
                protocolOutbox = get<ProtocolOutbox>(),
                send = { item -> sender.send(item) },
                deliveryStateListener = get()
            )
        }
        single<IncomingEnvelopeProcessor> {
            DefaultIncomingEnvelopeProcessor(
                resolveContactIdByRoutingId = get(),
                groupRoutingResolver = get(),
                reconcileContactTransportRouting = get(),
                localEncryptionKeyPairProvider = get(),
                incomingMessageHandler = get()
            )
        }
        single<IncomingEnvelopeGateway> {
            WebSocketIncomingEnvelopeGateway(webSocketTransportClient = get<WebSocketTransportClient>())
        }
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
