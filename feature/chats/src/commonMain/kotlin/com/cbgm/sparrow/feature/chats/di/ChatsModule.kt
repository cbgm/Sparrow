package com.cbgm.sparrow.feature.chats.di

import androidx.lifecycle.SavedStateHandle
import com.cbgm.sparrow.core.protocol.attachment.GroupPinnedAttachmentProvider
import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarProvider
import com.cbgm.sparrow.core.protocol.handler.IncomingMessageHandler
import com.cbgm.sparrow.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.sparrow.feature.chats.data.datasource.IncomingMessageDataSource
import com.cbgm.sparrow.feature.chats.data.datasource.MessageHistoryDataSource
import com.cbgm.sparrow.feature.chats.data.datasource.MessageReactionDataSource
import com.cbgm.sparrow.feature.chats.data.datasource.UnreadableTransportMessageDataSource
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectDeliveryDataSource
import com.cbgm.sparrow.feature.chats.data.direct.delivery.DirectMessageDeliveryCoordinator
import com.cbgm.sparrow.feature.chats.data.direct.delivery.DirectOutboxDeliveryHandler
import com.cbgm.sparrow.feature.chats.data.direct.incoming.DirectIncomingPacketProcessor
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectMessageDeletionPacketHandler
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectMessageEditPacketHandler
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectMessagePacketHandler
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.data.direct.repository.DirectConversationRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.direct.repository.DirectMessageRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.avatar.GroupAvatarBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.avatar.GroupAvatarPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupConversationDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupConversationHistoryDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupDescriptionDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupIncomingConversationDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupLocalCleanupDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupOutgoingMessageDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupRecipientDeliveryDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupTitleDataSource
import com.cbgm.sparrow.feature.chats.data.group.delivery.GroupMessageDeliveryCoordinator
import com.cbgm.sparrow.feature.chats.data.group.delivery.GroupOutboxDeliveryHandler
import com.cbgm.sparrow.feature.chats.data.group.description.GroupDescriptionBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.description.GroupDescriptionPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupCreatedIncomingProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupIncomingPacketPolicy
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupIncomingPacketProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupPacketHandlerRegistry
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupWelcomePersistence
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupAvatarUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupChatMessagePacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupDescriptionUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMessageDeletionPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMessageEditPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupPinUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupTitleUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.pin.GroupPinBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.pin.GroupPinPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupAvatarRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupConversationRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupDescriptionRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupKeyRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupMessageRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupPinRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupTitleRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupVerificationRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.title.GroupTitleBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.title.GroupTitlePacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationDataSource
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationPayloadEncoder
import com.cbgm.sparrow.feature.chats.data.incoming.IncomingPacketProcessor
import com.cbgm.sparrow.feature.chats.data.incoming.IncomingPacketRouter
import com.cbgm.sparrow.feature.chats.data.incoming.ReceiptIncomingPacketRouter
import com.cbgm.sparrow.feature.chats.data.orchestration.ChatsConversationPort
import com.cbgm.sparrow.feature.chats.data.outbox.ChatOutboxDeliveryStateRouter
import com.cbgm.sparrow.feature.chats.data.overview.datasource.ConversationOverviewDataSource
import com.cbgm.sparrow.feature.chats.data.overview.repository.ConversationOverviewRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.repository.DirectIndicatorRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.repository.GroupIndicatorRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.repository.MessageHistoryRepositoryImpl
import com.cbgm.sparrow.feature.chats.domain.repository.MessageHistoryRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectIndicatorRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupDescriptionRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupIndicatorRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupKeyRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupTitleRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationActionRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.sparrow.feature.chats.domain.usecase.FindMessageHistoryCursorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.contact.EncodeContactForSharingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ActivateAuthorizedDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DeleteDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DeleteDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DiscardPendingAuthorizationMessagesUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.EditDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.GetOrCreateDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.MarkDirectConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectIndicatorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.QueueDirectMessageUntilAuthorizedUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.RetryDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SendDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SendOrQueueDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SetDirectIndicatorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ToggleDirectMessageReactionUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.ForwardDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.ForwardMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.ForwardToContactUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.ForwardToDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.ForwardToGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.LoadOlderMessagesUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.PrepareForwardMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.AddGroupMembersUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.CreateGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeleteGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.EditGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.LoadGroupPinnedAttachmentUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.MarkGroupConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupDetailsContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupMemberIndicatorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupVerificationContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupVerificationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.PinGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RemoveGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RetryGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SendGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupDescriptionUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupIndicatorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupTitleUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SynchronizeGroupVerificationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ToggleGroupMessageReactionUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.UnpinGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.VerifyGroupMemberUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewsUseCase
import com.cbgm.sparrow.feature.chats.presentation.ContactsFlowViewModel
import com.cbgm.sparrow.feature.chats.presentation.create.CreateGroupViewModel
import com.cbgm.sparrow.feature.chats.presentation.details.GroupVerificationViewModel
import com.cbgm.sparrow.feature.chats.presentation.direct.DirectConversationViewModel
import com.cbgm.sparrow.feature.chats.presentation.forwarding.ForwardingSelectionViewModel
import com.cbgm.sparrow.feature.chats.presentation.group.GroupConversationViewModel
import com.cbgm.sparrow.feature.chats.presentation.overview.OverviewViewModel
import com.cbgm.sparrow.feature.chats.presentation.verification.GroupMemberQrVerificationViewModel
import com.cbgm.sparrow.feature.chats.runtime.group.incoming.GroupVerificationReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.runtime.group.incoming.GroupVerificationSnapshotPacketHandler
import com.cbgm.sparrow.feature.chats.runtime.group.incoming.GroupVerificationSnapshotRequestPacketHandler
import com.cbgm.sparrow.feature.chats.runtime.group.verification.GroupVerificationActionRepositoryImpl
import com.cbgm.sparrow.feature.chats.runtime.group.verification.GroupVerificationCoordinator
import com.cbgm.sparrow.feature.chats.runtime.group.verification.GroupVerificationSnapshotSender
import com.cbgm.sparrow.feature.chats.runtime.group.verification.GroupVerificationState
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.identity.domain.usecase.RecordLocalIdentitySharedUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatsModule =
    module {
        single<DirectIndicatorRepository> {
            DirectIndicatorRepositoryImpl(
                sendConversationIndicator = get(),
                observeConversationIndicator = get()
            )
        }
        single<GroupIndicatorRepository> {
            GroupIndicatorRepositoryImpl(
                sendConversationIndicator = get(),
                observeConversationIndicator = get()
            )
        }
        registerDirectData()
        registerGroupData()
        registerIncomingRouting()
        registerRepositories()
        registerUseCases()
        registerViewModels()
    }

private fun org.koin.core.module.Module.registerDirectData() {
    singleOf(::DirectConversationDataSource)
    singleOf(::DirectDeliveryDataSource)
    singleOf(::IncomingMessageDataSource)
    singleOf(::MessageReactionDataSource)
    singleOf(::DirectMessageDeliveryCoordinator)
    singleOf(::DirectOutboxDeliveryHandler)
    singleOf(::DirectOutgoingMessageProcessor)
    singleOf(::DirectPendingAuthorizationMessageCoordinator)
    singleOf(::DirectMessagePacketHandler)
    singleOf(::DirectMessageDeletionPacketHandler)
    singleOf(::DirectMessageEditPacketHandler)
    singleOf(::DirectReceiptPacketHandler)
    singleOf(::DirectIncomingPacketProcessor)

    singleOf(::ChatsConversationPort) {
        bind<ConversationPort>()
    }
}

private fun org.koin.core.module.Module.registerGroupData() {
    singleOf(::GroupConversationDataSource)
    singleOf(::GroupConversationHistoryDataSource)
    singleOf(::GroupIncomingConversationDataSource)
    singleOf(::GroupRecipientDeliveryDataSource)
    singleOf(::GroupMessageDeliveryCoordinator)
    singleOf(::GroupOutboxDeliveryHandler)
    singleOf(::GroupAvatarDataSource)
    singleOf(::GroupAvatarPacketProtocol)
    singleOf(::GroupAvatarBroadcaster)
    singleOf(::GroupDescriptionDataSource)
    singleOf(::GroupTitleDataSource)
    singleOf(::GroupDescriptionPacketProtocol)
    singleOf(::GroupDescriptionBroadcaster)
    singleOf(::GroupTitlePacketProtocol)
    singleOf(::GroupTitleBroadcaster)
    singleOf(::GroupPinDataSource)
    singleOf(::GroupPinPacketProtocol)
    singleOf(::GroupPinBroadcaster)
    singleOf(::GroupVerificationDataSource)
    singleOf(::GroupVerificationPayloadEncoder)
    singleOf(::GroupVerificationState)
    singleOf(::GroupVerificationSnapshotSender)
    singleOf(::GroupVerificationCoordinator)
    singleOf(::GroupOutgoingMessageDataSource)
    singleOf(::GroupOutgoingMessageProcessor)
    singleOf(::GroupPacketBroadcaster)
    singleOf(::GroupLocalCleanupDataSource)
    singleOf(::GroupWelcomePersistence)
    singleOf(::GroupCreatedIncomingProcessor)
    singleOf(::GroupIncomingPacketPolicy)

    singleOf(::GroupAvatarUpdatedPacketHandler)
    singleOf(::GroupDescriptionUpdatedPacketHandler)
    singleOf(::GroupTitleUpdatedPacketHandler)
    singleOf(::GroupPinUpdatedPacketHandler)
    singleOf(::GroupReceiptPacketHandler)
    singleOf(::GroupVerificationReceiptPacketHandler)
    singleOf(::GroupVerificationSnapshotRequestPacketHandler)
    singleOf(::GroupVerificationSnapshotPacketHandler)
    singleOf(::GroupChatMessagePacketHandler)
    singleOf(::GroupMessageDeletionPacketHandler)
    singleOf(::GroupMessageEditPacketHandler)
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
        bind<GroupAvatarProvider>()
    }
    singleOf(::GroupDescriptionRepositoryImpl) {
        bind<GroupDescriptionRepository>()
    }
    singleOf(::GroupTitleRepositoryImpl) {
        bind<GroupTitleRepository>()
    }
    singleOf(::GroupPinRepositoryImpl) {
        bind<GroupPinRepository>()
        bind<GroupPinnedAttachmentProvider>()
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
    singleOf(::ConversationOverviewDataSource)
    singleOf(::ConversationOverviewRepositoryImpl) {
        bind<ConversationOverviewRepository>()
    }
    singleOf(::MessageHistoryDataSource)
    singleOf(::MessageHistoryRepositoryImpl) {
        bind<MessageHistoryRepository>()
    }
}

private fun org.koin.core.module.Module.registerUseCases() {
    singleOf(::PrepareForwardMessageUseCase)
    singleOf(::ForwardDirectMessageUseCase)
    singleOf(::ForwardToDirectConversationUseCase)
    singleOf(::ForwardToGroupConversationUseCase)
    singleOf(::ForwardToContactUseCase)
    singleOf(::ForwardMessageUseCase)
    singleOf(::LoadOlderMessagesUseCase)
    singleOf(::FindMessageHistoryCursorUseCase)
    singleOf(::EncodeContactForSharingUseCase)
    singleOf(::GetOrCreateDirectConversationUseCase)
    singleOf(::ObserveDirectConversationUseCase)
    singleOf(::ObserveDirectChatContextUseCase)
    singleOf(::SendDirectMessageUseCase)
    singleOf(::SendOrQueueDirectMessageUseCase)
    singleOf(::ToggleDirectMessageReactionUseCase)
    singleOf(::DeleteDirectMessageUseCase)
    singleOf(::EditDirectMessageUseCase)
    singleOf(::QueueDirectMessageUntilAuthorizedUseCase)
    singleOf(::RetryDirectMessageUseCase)
    singleOf(::MarkDirectConversationReadUseCase)
    singleOf(::ObserveDirectIndicatorUseCase)
    singleOf(::SetDirectIndicatorUseCase)
    singleOf(::ActivateAuthorizedDirectConversationUseCase)
    singleOf(::DiscardPendingAuthorizationMessagesUseCase)
    singleOf(::DeleteDirectConversationUseCase)

    singleOf(::AddGroupMembersUseCase)
    singleOf(::CreateGroupConversationUseCase)
    singleOf(::ObserveGroupConversationUseCase)
    singleOf(::ObserveGroupChatContextUseCase)
    singleOf(::ObserveGroupDetailsContextUseCase)
    singleOf(::SendGroupMessageUseCase)
    singleOf(::ToggleGroupMessageReactionUseCase)
    singleOf(::DeleteGroupMessageUseCase)
    singleOf(::EditGroupMessageUseCase)
    singleOf(::RetryGroupMessageUseCase)
    singleOf(::MarkGroupConversationReadUseCase)
    singleOf(::SetGroupAvatarUseCase)
    singleOf(::RemoveGroupAvatarUseCase)
    singleOf(::SetGroupTitleUseCase)
    singleOf(::SetGroupDescriptionUseCase)
    singleOf(::LoadGroupPinnedAttachmentUseCase)
    singleOf(::PinGroupMessageUseCase)
    singleOf(::UnpinGroupMessageUseCase)
    singleOf(::ObserveGroupMemberIndicatorUseCase)
    singleOf(::SetGroupIndicatorUseCase)
    singleOf(::ObserveGroupVerificationContextUseCase)
    singleOf(::ObserveGroupVerificationUseCase)
    singleOf(::SynchronizeGroupVerificationUseCase)
    singleOf(::VerifyGroupMemberUseCase)

    singleOf(::ObserveConversationOverviewsUseCase)
    singleOf(::ObserveConversationOverviewContextUseCase)
}

private fun org.koin.core.module.Module.registerViewModels() {
    viewModel {
        ContactsFlowViewModel(prepareConversationOpen = get())
    }

    viewModel {
        OverviewViewModel(
            observeConversationContext = get(),
            observeActiveAutoReply = get(),
            deletePeerConversation = get(),
            deleteGroupConversation = get(),
            getGroupLeaveRequirement = get()
        )
    }

    viewModel {
        CreateGroupViewModel(
            savedStateHandle = get(),
            observeContacts = get(),
            createGroupConversation = get()
        )
    }

    viewModel {
        ForwardingSelectionViewModel(
            observeConversationContext = get(),
            observeContacts = get()
        )
    }

    viewModel {
        GroupConversationViewModel(
            savedStateHandle = get(),
            observeChatContext = get(),
            sendMessage = get(),
            markConversationRead = get(),
            retryMessage = get(),
            toggleMessageReaction = get(),
            deleteMessageUseCase = get(),
            editMessageUseCase = get(),
            pinMessageUseCase = get(),
            unpinMessageUseCase = get(),
            observeMemberIndicator = get(),
            setGroupIndicator = get(),
            observeMessageSafetyAssessments = get(),
            addDeviceContact = get(),
            forwardMessageUseCase = get(),
            loadOlderMessageHistory = get(),
            findMessageHistoryCursor = get(),
            getRecordedVoiceAttachment = get(),
            resetVoiceComposer = get(),
            observeVoiceRecordingActive = get(),
            mediaFiles = get()
        )
    }

    viewModel {
        GroupVerificationViewModel(
            savedStateHandle = get(),
            observeGroupDetailsContext = get(),
            synchronizeGroupVerification = get(),
            verifyGroupMember = get(),
            getContactSafetyNumber = get<GetContactSafetyNumberUseCase>(),
            observeContacts = get(),
            addGroupMembers = get(),
            removeGroupMember = get(),
            promoteGroupMember = get(),
            transferGroupAdminAndLeave = get(),
            consumeAvatarEditResult = get(),
            setGroupAvatar = get(),
            removeGroupAvatar = get(),
            setGroupTitle = get(),
            setGroupDescription = get(),
            getGroupLeaveRequirement = get(),
            leaveGroup = get()
        )
    }

    viewModel {
        GroupMemberQrVerificationViewModel(
            savedStateHandle = get(),
            decodeSharedIdentity = get(),
            getRemoteIdentity = get(),
            verifyGroupMember = get()
        )
    }

    viewModel { (savedStateHandle: SavedStateHandle) ->
        DirectConversationViewModel(
            savedStateHandle = savedStateHandle,
            observeChatContext = get(),
            sendOrQueueDirectMessage = get(),
            recoverManualIdentityExchange = get(),
            recordLocalIdentityShared = get<RecordLocalIdentitySharedUseCase>(),
            markConversationRead = get(),
            retryMessage = get(),
            toggleMessageReaction = get(),
            deleteMessageUseCase = get(),
            editMessageUseCase = get(),
            observeIndicator = get(),
            setIndicator = get(),
            observeMessageSafetyAssessments = get(),
            addDeviceContact = get(),
            forwardMessageUseCase = get(),
            loadOlderMessageHistory = get(),
            findMessageHistoryCursor = get(),
            getRecordedVoiceAttachment = get(),
            resetVoiceComposer = get(),
            observeVoiceRecordingActive = get(),
            mediaFiles = get()
        )
    }
}
