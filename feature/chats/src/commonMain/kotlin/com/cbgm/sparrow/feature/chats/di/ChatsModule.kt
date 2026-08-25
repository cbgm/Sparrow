package com.cbgm.sparrow.feature.chats.di

import com.cbgm.sparrow.core.protocol.handler.IncomingMessageHandler
import com.cbgm.sparrow.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.sparrow.feature.chats.data.attachment.MessageAttachmentCacheCoordinator
import com.cbgm.sparrow.feature.chats.data.attachment.MessageAttachmentTransfer
import com.cbgm.sparrow.feature.chats.data.attachment.repository.MessageAttachmentRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.datasource.UnreadableTransportMessageDataSource
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.feature.chats.data.direct.delivery.DirectMessageDeliveryCoordinator
import com.cbgm.sparrow.feature.chats.data.direct.delivery.DirectOutboxDeliveryHandler
import com.cbgm.sparrow.feature.chats.data.direct.incoming.DirectIncomingPacketProcessor
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectMessagePacketHandler
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.data.direct.invitation.DirectInvitationConversationCoordinator
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.data.direct.repository.DirectConversationRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.direct.repository.DirectMessageRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.avatar.GroupAvatarBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.avatar.GroupAvatarPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupLocalCleanupDataSource
import com.cbgm.sparrow.feature.chats.data.group.delivery.GroupMessageDeliveryCoordinator
import com.cbgm.sparrow.feature.chats.data.group.delivery.GroupOutboxDeliveryHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupCreatedIncomingProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupIncomingPacketPolicy
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupIncomingPacketProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupInviteDeclinedIncomingProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupInviteIncomingProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupInviteReceivedIncomingProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupJoinRequestIncomingProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupPacketHandlerRegistry
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupWelcomeMembershipResolver
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupWelcomePersistence
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupWelcomeSecurityProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupAvatarUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupChatMessagePacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupConversationDeletedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupCreatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupInviteDeclinedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupInvitePacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupInviteReceivedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupJoinRequestPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupLeaveRequestPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMemberActivatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMemberActivationAcknowledgementPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMemberRemovedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupReadyAcknowledgementPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupVerificationReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupVerificationSnapshotPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupVerificationSnapshotRequestPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupEpochCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupInvitationCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupLeaveCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMemberPromotionCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMemberRemovalCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipActivationCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipAdministrationCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipDeletionCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipIdentity
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipLock
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupProtocolPayloadEncoder
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupAvatarRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupConversationRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupKeyRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupMembershipRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupMessageRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupVerificationActionRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupVerificationRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.data.group.security.GroupWelcomeSecurity
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationCoordinator
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationPayloadEncoder
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationSnapshotSender
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationState
import com.cbgm.sparrow.feature.chats.data.incoming.IncomingPacketProcessor
import com.cbgm.sparrow.feature.chats.data.incoming.IncomingPacketRouter
import com.cbgm.sparrow.feature.chats.data.incoming.ReceiptIncomingPacketRouter
import com.cbgm.sparrow.feature.chats.data.outbox.ChatOutboxDeliveryStateRouter
import com.cbgm.sparrow.feature.chats.data.overview.repository.ConversationOverviewRepositoryImpl
import com.cbgm.sparrow.feature.chats.domain.repository.attachment.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupKeyRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationActionRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.sparrow.feature.chats.domain.usecase.attachment.LoadMessageAttachmentUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.contact.EncodeContactForSharingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DeleteDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.GetOrCreateDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.HandleAcceptedDirectInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.HandleDeclinedDirectInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.MarkDirectConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveAcceptedDirectInvitationsUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDeclinedDirectInvitationsUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.QueueDirectMessageUntilAuthorizedUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.RetryDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SendDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SetDirectTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.AcceptGroupInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.AddGroupMembersUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.CreateGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeclineGroupInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeleteGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.LeaveGroupUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.MarkGroupConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupAdministrationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupAvatarsUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupDetailsContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupMemberTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupVerificationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.PromoteGroupMemberUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RemoveGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RemoveGroupMemberUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RetryGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SendGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SynchronizeGroupVerificationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.TransferGroupAdminAndLeaveUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.VerifyGroupMemberUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.incoming.EstablishGroupMemberIdentityUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.incoming.MarkGroupContactIdentityMutualUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.incoming.StageIncomingGroupOwnerIdentityUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewsUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.profile.ObserveRemoteProfilePicturesUseCase
import com.cbgm.sparrow.feature.chats.presentation.ContactsFlowViewModel
import com.cbgm.sparrow.feature.chats.presentation.create.CreateGroupViewModel
import com.cbgm.sparrow.feature.chats.presentation.details.GroupVerificationViewModel
import com.cbgm.sparrow.feature.chats.presentation.direct.screen.DirectViewModel
import com.cbgm.sparrow.feature.chats.presentation.group.screen.GroupViewModel
import com.cbgm.sparrow.feature.chats.presentation.overview.OverviewViewModel
import com.cbgm.sparrow.feature.chats.presentation.verification.GroupMemberQrVerificationViewModel
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
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
    singleOf(::MessageAttachmentTransfer)
    singleOf(::MessageAttachmentCacheCoordinator)
    singleOf(::DirectConversationDataSource)
    singleOf(::DirectMessageDeliveryCoordinator)
    singleOf(::DirectInvitationConversationCoordinator)
    singleOf(::DirectOutboxDeliveryHandler)
    singleOf(::DirectOutgoingMessageProcessor)
    singleOf(::DirectPendingAuthorizationMessageCoordinator)
    singleOf(::DirectMessagePacketHandler)
    singleOf(::DirectReceiptPacketHandler)
    singleOf(::DirectIncomingPacketProcessor)
}

private fun org.koin.core.module.Module.registerGroupData() {
    singleOf(::GroupMessageDeliveryCoordinator)
    singleOf(::GroupOutboxDeliveryHandler)
    singleOf(::GroupProtocolPayloadEncoder)
    singleOf(::GroupAvatarDataSource)
    singleOf(::GroupAvatarPacketProtocol)
    singleOf(::GroupAvatarBroadcaster)
    single {
        GroupMembershipPacketProtocol(
            groupCrypto = get(),
            payloadEncoder = get(),
            localProfilePictureMetadataProvider = get()
        )
    }
    singleOf(::GroupWelcomeSecurity)
    singleOf(::GroupSecurityManager)
    singleOf(::GroupVerificationPayloadEncoder)
    singleOf(::GroupVerificationState)
    singleOf(::GroupVerificationSnapshotSender)
    singleOf(::GroupVerificationCoordinator)
    singleOf(::GroupOutgoingMessageProcessor)
    singleOf(::GroupPacketBroadcaster)
    singleOf(::GroupMembershipLock)
    singleOf(::GroupLocalCleanupDataSource)
    singleOf(::GroupMembershipIdentity)
    singleOf(::GroupEpochCoordinator)
    singleOf(::GroupMembershipActivationCoordinator)
    singleOf(::GroupMemberPromotionCoordinator)
    singleOf(::GroupMemberRemovalCoordinator)
    singleOf(::GroupLeaveCoordinator)
    singleOf(::GroupMembershipAdministrationCoordinator)
    singleOf(::GroupMembershipDeletionCoordinator)
    singleOf(::GroupInvitationCoordinator)
    singleOf(::GroupMembershipCoordinator)
    singleOf(::GroupInviteIncomingProcessor)
    singleOf(::GroupInviteReceivedIncomingProcessor)
    singleOf(::GroupJoinRequestIncomingProcessor)
    singleOf(::GroupInviteDeclinedIncomingProcessor)
    singleOf(::GroupWelcomeMembershipResolver)
    singleOf(::GroupWelcomePersistence)
    singleOf(::GroupWelcomeSecurityProcessor)
    singleOf(::GroupCreatedIncomingProcessor)
    singleOf(::GroupIncomingPacketPolicy)

    singleOf(::GroupAvatarUpdatedPacketHandler)
    singleOf(::GroupCreatedPacketHandler)
    singleOf(::GroupConversationDeletedPacketHandler)
    singleOf(::GroupInvitePacketHandler)
    singleOf(::GroupInviteReceivedPacketHandler)
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
    singleOf(::UnreadableTransportMessageDataSource)
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
    singleOf(::MessageAttachmentRepositoryImpl) {
        bind<MessageAttachmentRepository>()
    }
    singleOf(::DirectConversationRepositoryImpl) {
        bind<DirectConversationRepository>()
    }
    singleOf(::DirectMessageRepositoryImpl) {
        bind<DirectMessageRepository>()
    }
    singleOf(::GroupConversationRepositoryImpl) {
        bind<GroupConversationRepository>()
    }
    singleOf(::GroupAvatarRepositoryImpl) {
        bind<GroupAvatarRepository>()
    }
    singleOf(::GroupMembershipRepositoryImpl) {
        bind<GroupMembershipRepository>()
    }
    singleOf(::GroupKeyRepositoryImpl) {
        bind<GroupKeyRepository>()
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
    singleOf(::LoadMessageAttachmentUseCase)
    singleOf(::EncodeContactForSharingUseCase)
    singleOf(::GetOrCreateDirectConversationUseCase)
    singleOf(::ObserveDirectConversationUseCase)
    singleOf(::ObserveDirectChatContextUseCase)
    singleOf(::SendDirectMessageUseCase)
    singleOf(::QueueDirectMessageUntilAuthorizedUseCase)
    singleOf(::RetryDirectMessageUseCase)
    singleOf(::MarkDirectConversationReadUseCase)
    singleOf(::DeleteDirectConversationUseCase)
    singleOf(::ObserveDirectTypingUseCase)
    singleOf(::SetDirectTypingUseCase)
    singleOf(::ObserveAcceptedDirectInvitationsUseCase)
    singleOf(::ObserveDeclinedDirectInvitationsUseCase)
    singleOf(::HandleAcceptedDirectInvitationUseCase)
    singleOf(::HandleDeclinedDirectInvitationUseCase)

    singleOf(::CreateGroupConversationUseCase)
    singleOf(::ObserveGroupConversationUseCase)
    singleOf(::ObserveGroupChatContextUseCase)
    singleOf(::ObserveGroupDetailsContextUseCase)
    singleOf(::SendGroupMessageUseCase)
    singleOf(::RetryGroupMessageUseCase)
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
    singleOf(::ObserveGroupAvatarUseCase)
    singleOf(::ObserveGroupAvatarsUseCase)
    singleOf(::SetGroupAvatarUseCase)
    singleOf(::RemoveGroupAvatarUseCase)
    singleOf(::ObserveGroupMemberTypingUseCase)
    singleOf(::SetGroupTypingUseCase)
    singleOf(::ObserveGroupVerificationUseCase)
    singleOf(::SynchronizeGroupVerificationUseCase)
    singleOf(::VerifyGroupMemberUseCase)
    singleOf(::StageIncomingGroupOwnerIdentityUseCase)
    singleOf(::EstablishGroupMemberIdentityUseCase)
    singleOf(::MarkGroupContactIdentityMutualUseCase)

    singleOf(::ObserveConversationOverviewsUseCase)
    singleOf(::ObserveConversationOverviewContextUseCase)
    singleOf(::ObserveRemoteProfilePicturesUseCase)
}

private fun org.koin.core.module.Module.registerViewModels() {
    viewModel {
        ContactsFlowViewModel(
            getOrCreateDirectConversation = get(),
            ensureIdentityExchangeStarted = get<EnsureIdentityExchangeStartedUseCase>(),
            observeIdentitySetupMode = get(),
            requireDirectChatAuthorization = get()
        )
    }

    viewModel {
        OverviewViewModel(
            observeConversationContext = get(),
            deleteDirectConversation = get(),
            deleteGroupConversation = get(),
            getGroupLeaveRequirement = get()
        )
    }

    viewModel {
        CreateGroupViewModel(
            savedStateHandle = get(),
            observeContactsWithProfilePictures = get(),
            createGroupConversation = get()
        )
    }

    viewModel {
        GroupViewModel(
            savedStateHandle = get(),
            observeChatContext = get(),
            sendMessage = get(),
            markConversationRead = get(),
            retryMessage = get(),
            acceptInvitation = get(),
            declineInvitation = get(),
            observeMemberTyping = get(),
            setGroupTyping = get(),
            observeMessageSafetyAssessments = get(),
            loadMessageAttachment = get()
        )
    }

    viewModel {
        GroupVerificationViewModel(
            savedStateHandle = get(),
            observeGroupDetailsContext = get(),
            synchronizeGroupVerification = get(),
            verifyGroupMember = get(),
            getContactSafetyNumber = get<GetContactSafetyNumberUseCase>(),
            observeContactsWithProfilePictures = get(),
            addGroupMembers = get(),
            removeGroupMember = get(),
            promoteGroupMember = get(),
            transferGroupAdminAndLeave = get(),
            setGroupAvatar = get(),
            removeGroupAvatar = get(),
            getGroupLeaveRequirement = get(),
            leaveGroup = get()
        )
    }

    viewModel {
        GroupMemberQrVerificationViewModel(
            savedStateHandle = get(),
            decodeSharedIdentity = get(),
            getContact = get(),
            verifyGroupMember = get()
        )
    }

    viewModel {
        DirectViewModel(
            savedStateHandle = get(),
            observeChatContext = get(),
            sendMessage = get(),
            queueMessageUntilAuthorized = get(),
            markConversationRead = get(),
            retryMessage = get(),
            ensureIdentityExchangeStarted = get<EnsureIdentityExchangeStartedUseCase>(),
            requireDirectChatAuthorization = get<RequireDirectChatAuthorizationUseCase>(),
            observeTyping = get(),
            setTyping = get(),
            observeMessageSafetyAssessments = get(),
            loadMessageAttachment = get()
        )
    }
}
