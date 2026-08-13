package com.cbgm.securechat.feature.chats.di

import com.cbgm.securechat.core.protocol.handler.IncomingMessageHandler
import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.feature.chats.data.direct.delivery.DirectMessageDeliveryCoordinator
import com.cbgm.securechat.feature.chats.data.direct.delivery.DirectOutboxDeliveryHandler
import com.cbgm.securechat.feature.chats.data.direct.incoming.DirectIncomingPacketProcessor
import com.cbgm.securechat.feature.chats.data.direct.incoming.handler.DirectMessagePacketHandler
import com.cbgm.securechat.feature.chats.data.direct.incoming.handler.DirectReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.direct.outgoing.DirectOutgoingMessageProcessor
import com.cbgm.securechat.feature.chats.data.direct.repository.DirectConversationRepositoryImpl
import com.cbgm.securechat.feature.chats.data.direct.repository.DirectMessageRepositoryImpl
import com.cbgm.securechat.feature.chats.data.direct.storage.DirectConversationStorage
import com.cbgm.securechat.feature.chats.data.group.delivery.GroupMessageDeliveryCoordinator
import com.cbgm.securechat.feature.chats.data.group.delivery.GroupOutboxDeliveryHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.GroupIncomingPacketPolicy
import com.cbgm.securechat.feature.chats.data.group.incoming.GroupIncomingPacketProcessor
import com.cbgm.securechat.feature.chats.data.group.incoming.GroupPacketHandlerRegistry
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupChatMessagePacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupConversationDeletedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupCreatedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupInviteDeclinedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupInvitePacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupJoinRequestPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupLeaveRequestPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupMemberActivatedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupMemberActivationAcknowledgementPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupMemberRemovedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupReadyAcknowledgementPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupVerificationReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupVerificationSnapshotPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupVerificationSnapshotRequestPacketHandler
import com.cbgm.securechat.feature.chats.data.group.membership.GroupEpochCoordinator
import com.cbgm.securechat.feature.chats.data.group.membership.GroupInvitationCoordinator
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipActivationCoordinator
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipAdministrationCoordinator
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipCoordinator
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipDeletionCoordinator
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipIdentity
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipLock
import com.cbgm.securechat.feature.chats.data.group.outgoing.GroupOutgoingMessageProcessor
import com.cbgm.securechat.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.securechat.feature.chats.data.group.protocol.GroupProtocolPayloadEncoder
import com.cbgm.securechat.feature.chats.data.group.repository.GroupConversationRepositoryImpl
import com.cbgm.securechat.feature.chats.data.group.repository.GroupMembershipRepositoryImpl
import com.cbgm.securechat.feature.chats.data.group.repository.GroupMessageRepositoryImpl
import com.cbgm.securechat.feature.chats.data.group.repository.GroupVerificationActionRepositoryImpl
import com.cbgm.securechat.feature.chats.data.group.repository.GroupVerificationRepositoryImpl
import com.cbgm.securechat.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.group.security.GroupWelcomeSecurity
import com.cbgm.securechat.feature.chats.data.group.storage.GroupLocalDataCleaner
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationCoordinator
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationPayloadEncoder
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationSnapshotSender
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationState
import com.cbgm.securechat.feature.chats.data.incoming.IncomingPacketProcessor
import com.cbgm.securechat.feature.chats.data.incoming.IncomingPacketRouter
import com.cbgm.securechat.feature.chats.data.incoming.ReceiptIncomingPacketRouter
import com.cbgm.securechat.feature.chats.data.outbox.ChatOutboxDeliveryStateRouter
import com.cbgm.securechat.feature.chats.data.overview.repository.ConversationOverviewRepositoryImpl
import com.cbgm.securechat.feature.chats.data.storage.UnreadableTransportMessageStorage
import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectMessageRepository
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMembershipRepository
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMessageRepository
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupVerificationActionRepository
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupVerificationRepository
import com.cbgm.securechat.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.securechat.feature.chats.domain.usecase.direct.DeleteDirectConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.GetOrCreateDirectConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.MarkDirectConversationReadUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.ObserveDirectConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.ObserveDirectTypingUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.RefreshDirectDeliveryStateUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.RetryDirectMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.SendDirectMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.SetDirectTypingUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.AcceptGroupInvitationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.AddGroupMembersUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.CreateGroupConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.DeclineGroupInvitationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.DeleteGroupConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.GetGroupLeaveRequirementUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.LeaveGroupUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.MarkGroupConversationReadUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.ObserveGroupAdministrationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.ObserveGroupConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.ObserveGroupMemberTypingUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.ObserveGroupVerificationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.PromoteGroupMemberUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.RefreshGroupDeliveryStateUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.RemoveGroupMemberUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.RetryGroupMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.SendGroupMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.SetGroupTypingUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.SynchronizeGroupVerificationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.TransferGroupAdminAndLeaveUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.VerifyGroupMemberUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.overview.ObserveConversationOverviewsUseCase
import com.cbgm.securechat.feature.chats.presentation.ContactsFlowViewModel
import com.cbgm.securechat.feature.chats.presentation.create.CreateGroupViewModel
import com.cbgm.securechat.feature.chats.presentation.details.GroupVerificationViewModel
import com.cbgm.securechat.feature.chats.presentation.direct.screen.DirectViewModel
import com.cbgm.securechat.feature.chats.presentation.group.screen.GroupViewModel
import com.cbgm.securechat.feature.chats.presentation.overview.OverviewViewModel
import com.cbgm.securechat.feature.chats.presentation.verification.GroupMemberQrVerificationViewModel
import com.cbgm.securechat.feature.contacts.domain.usecase.EnsureIdentityExchangeStarted
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactSafetyNumber
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveIdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveIdentitySetupMode
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatsModule =
    module {
        registerDirectData()
        registerGroupData()
        registerIncomingRouting()
        registerRepositories()
        registerUseCases()
        registerViewModels()
    }

private fun org.koin.core.module.Module.registerDirectData() {
    singleOf(::DirectConversationStorage)
    singleOf(::DirectMessageDeliveryCoordinator)
    singleOf(::DirectOutboxDeliveryHandler)
    singleOf(::DirectOutgoingMessageProcessor)
    singleOf(::DirectMessagePacketHandler)
    singleOf(::DirectReceiptPacketHandler)
    singleOf(::DirectIncomingPacketProcessor)
}

private fun org.koin.core.module.Module.registerGroupData() {
    singleOf(::GroupMessageDeliveryCoordinator)
    singleOf(::GroupOutboxDeliveryHandler)
    singleOf(::GroupProtocolPayloadEncoder)
    singleOf(::GroupMembershipPacketProtocol)
    singleOf(::GroupWelcomeSecurity)
    singleOf(::GroupSecurityManager)
    singleOf(::GroupVerificationPayloadEncoder)
    singleOf(::GroupVerificationState)
    singleOf(::GroupVerificationSnapshotSender)
    singleOf(::GroupVerificationCoordinator)
    singleOf(::GroupOutgoingMessageProcessor)
    singleOf(::GroupMembershipLock)
    singleOf(::GroupLocalDataCleaner)
    singleOf(::GroupMembershipIdentity)
    singleOf(::GroupEpochCoordinator)
    singleOf(::GroupMembershipActivationCoordinator)
    singleOf(::GroupMembershipAdministrationCoordinator)
    singleOf(::GroupMembershipDeletionCoordinator)
    singleOf(::GroupInvitationCoordinator)
    singleOf(::GroupMembershipCoordinator)
    singleOf(::GroupIncomingPacketPolicy)

    singleOf(::GroupCreatedPacketHandler)
    singleOf(::GroupConversationDeletedPacketHandler)
    singleOf(::GroupInvitePacketHandler)
    singleOf(::GroupJoinRequestPacketHandler)
    singleOf(::GroupLeaveRequestPacketHandler)
    singleOf(::GroupInviteDeclinedPacketHandler)
    singleOf(::GroupReadyAcknowledgementPacketHandler)
    singleOf(::GroupReceiptPacketHandler)
    singleOf(::GroupMemberActivatedPacketHandler)
    singleOf(::GroupMemberActivationAcknowledgementPacketHandler)
    singleOf(::GroupMemberRemovedPacketHandler)
    singleOf(::GroupVerificationReceiptPacketHandler)
    singleOf(::GroupVerificationSnapshotRequestPacketHandler)
    singleOf(::GroupVerificationSnapshotPacketHandler)
    singleOf(::GroupChatMessagePacketHandler)
    singleOf(::GroupPacketHandlerRegistry)
    singleOf(::GroupIncomingPacketProcessor)
}

private fun org.koin.core.module.Module.registerIncomingRouting() {
    singleOf(::UnreadableTransportMessageStorage)
    singleOf(::ReceiptIncomingPacketRouter)
    singleOf(::IncomingPacketRouter)
    singleOf(::IncomingPacketProcessor) {
        bind<IncomingMessageHandler>()
    }
    singleOf(::ChatOutboxDeliveryStateRouter) {
        bind<OutboxDeliveryStateListener>()
    }
}

private fun org.koin.core.module.Module.registerRepositories() {
    singleOf(::DirectConversationRepositoryImpl) {
        bind<DirectConversationRepository>()
    }
    singleOf(::DirectMessageRepositoryImpl) {
        bind<DirectMessageRepository>()
    }
    singleOf(::GroupConversationRepositoryImpl) {
        bind<GroupConversationRepository>()
    }
    singleOf(::GroupMembershipRepositoryImpl) {
        bind<GroupMembershipRepository>()
    }
    singleOf(::GroupMessageRepositoryImpl) {
        bind<GroupMessageRepository>()
    }
    singleOf(::GroupVerificationActionRepositoryImpl) {
        bind<GroupVerificationActionRepository>()
    }
    singleOf(::GroupVerificationRepositoryImpl) {
        bind<GroupVerificationRepository>()
    }
    singleOf(::ConversationOverviewRepositoryImpl) {
        bind<ConversationOverviewRepository>()
    }
}

private fun org.koin.core.module.Module.registerUseCases() {
    singleOf(::GetOrCreateDirectConversationUseCase)
    singleOf(::ObserveDirectConversationUseCase)
    singleOf(::SendDirectMessageUseCase)
    singleOf(::RetryDirectMessageUseCase)
    singleOf(::RefreshDirectDeliveryStateUseCase)
    singleOf(::MarkDirectConversationReadUseCase)
    singleOf(::DeleteDirectConversationUseCase)
    singleOf(::ObserveDirectTypingUseCase)
    singleOf(::SetDirectTypingUseCase)

    singleOf(::CreateGroupConversationUseCase)
    singleOf(::ObserveGroupConversationUseCase)
    singleOf(::SendGroupMessageUseCase)
    singleOf(::RetryGroupMessageUseCase)
    singleOf(::RefreshGroupDeliveryStateUseCase)
    singleOf(::MarkGroupConversationReadUseCase)
    singleOf(::DeleteGroupConversationUseCase)
    singleOf(::AcceptGroupInvitationUseCase)
    singleOf(::DeclineGroupInvitationUseCase)
    singleOf(::AddGroupMembersUseCase)
    singleOf(::RemoveGroupMemberUseCase)
    singleOf(::PromoteGroupMemberUseCase)
    singleOf(::TransferGroupAdminAndLeaveUseCase)
    singleOf(::GetGroupLeaveRequirementUseCase)
    singleOf(::LeaveGroupUseCase)
    singleOf(::ObserveGroupAdministrationUseCase)
    singleOf(::ObserveGroupMemberTypingUseCase)
    singleOf(::SetGroupTypingUseCase)
    singleOf(::ObserveGroupVerificationUseCase)
    singleOf(::SynchronizeGroupVerificationUseCase)
    singleOf(::VerifyGroupMemberUseCase)

    singleOf(::ObserveConversationOverviewsUseCase)
}

private fun org.koin.core.module.Module.registerViewModels() {
    viewModel {
        ContactsFlowViewModel(
            getOrCreateDirectConversation = get(),
            ensureIdentityExchangeStarted = get<EnsureIdentityExchangeStarted>()
        )
    }

    viewModel {
        OverviewViewModel(
            observeConversations = get(),
            deleteDirectConversation = get(),
            deleteGroupConversation = get(),
            getGroupLeaveRequirement = get()
        )
    }

    viewModel {
        CreateGroupViewModel(
            observeContacts = get(),
            createGroupConversation = get()
        )
    }

    viewModel { parameters ->
        GroupViewModel(
            groupId = parameters.get(),
            observeConversation = get(),
            observeAdministration = get(),
            observeVerification = get(),
            sendMessage = get(),
            markConversationRead = get(),
            retryMessage = get(),
            refreshDeliveryState = get(),
            acceptInvitation = get(),
            declineInvitation = get(),
            observeContacts = get<ObserveContacts>(),
            observeMemberTyping = get(),
            setGroupTyping = get()
        )
    }

    viewModel { parameters ->
        GroupVerificationViewModel(
            conversationId = parameters.get(),
            observeGroupVerification = get(),
            synchronizeGroupVerification = get(),
            verifyGroupMember = get(),
            getContactSafetyNumber = get<GetContactSafetyNumber>(),
            observeContacts = get(),
            addGroupMembers = get(),
            removeGroupMember = get(),
            promoteGroupMember = get(),
            transferGroupAdminAndLeave = get(),
            observeGroupAdministration = get(),
            leaveGroup = get()
        )
    }

    viewModel { parameters ->
        GroupMemberQrVerificationViewModel(
            groupId = parameters.get(),
            contactId = parameters.get(),
            decodeSharedIdentity = get(),
            getContact = get(),
            verifyGroupMember = get()
        )
    }

    viewModel { parameters ->
        DirectViewModel(
            conversationId = parameters.get(),
            contactId = parameters.get(),
            fallbackContactName = parameters.get(),
            observeConversation = get(),
            sendMessage = get(),
            markConversationRead = get(),
            retryMessage = get(),
            refreshDeliveryState = get(),
            observeIdentitySetupMode = get<ObserveIdentitySetupMode>(),
            ensureIdentityExchangeStarted = get<EnsureIdentityExchangeStarted>(),
            observeIdentityHandshakeState = get<ObserveIdentityHandshakeState>(),
            observeContact = get<ObserveContact>(),
            observeTyping = get(),
            setTyping = get()
        )
    }
}
