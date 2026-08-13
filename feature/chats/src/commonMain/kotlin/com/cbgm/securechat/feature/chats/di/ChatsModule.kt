package com.cbgm.securechat.feature.chats.di

import com.cbgm.securechat.core.protocol.handler.IncomingMessageHandler
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.feature.chats.data.conversation.DirectConversationStore
import com.cbgm.securechat.feature.chats.data.delivery.MessageDeliveryStateCoordinator
import com.cbgm.securechat.feature.chats.data.incoming.IncomingMessageProcessor
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationCoordinator
import com.cbgm.securechat.feature.chats.data.message.GroupMessageSender
import com.cbgm.securechat.feature.chats.data.outbox.ChatOutboxDeliveryStateListener
import com.cbgm.securechat.feature.chats.data.protocol.ChatMessagePacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.DeliveryReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupChatMessagePacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupConversationDeletedPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupCreatedPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupInviteDeclinedPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupInvitePacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupJoinRequestPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupLeaveRequestPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupMemberActivatedPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupMemberActivationAcknowledgementPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupMemberRemovedPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupReadyAcknowledgementPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupVerificationReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupVerificationSnapshotPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupVerificationSnapshotRequestPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.ReadReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.repository.DefaultChatsRepository
import com.cbgm.securechat.feature.chats.data.repository.DefaultGroupVerificationRepository
import com.cbgm.securechat.feature.chats.data.security.GroupInvitationManager
import com.cbgm.securechat.feature.chats.data.security.GroupProtocolPayloadEncoder
import com.cbgm.securechat.feature.chats.data.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.verification.GroupVerificationCoordinator
import com.cbgm.securechat.feature.chats.data.verification.GroupVerificationPayloadEncoder
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationActionRepository
import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationRepository
import com.cbgm.securechat.feature.chats.domain.usecase.AcceptGroupInvitationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.AddGroupMembersUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.CreateGroupConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.DeclineGroupInvitationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.DeleteConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.GetGroupLeaveRequirementUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.GetOrCreateDirectConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.LeaveGroupUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.MarkConversationReadUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversationsUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupAdministrationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupVerificationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveTypingIndicatorUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.PromoteGroupMemberUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.RefreshDeliveryStateUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.RemoveGroupMemberUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.RetryMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.SendGroupMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.SendMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.SetTypingIndicatorUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.SynchronizeGroupVerificationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.TransferGroupAdminAndLeaveUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.VerifyGroupMemberUseCase
import com.cbgm.securechat.feature.chats.presentation.ContactsFlowViewModel
import com.cbgm.securechat.feature.chats.presentation.create.CreateGroupViewModel
import com.cbgm.securechat.feature.chats.presentation.details.GroupVerificationViewModel
import com.cbgm.securechat.feature.chats.presentation.direct.DirectViewModel
import com.cbgm.securechat.feature.chats.presentation.group.GroupViewModel
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

        singleOf(::MessageDeliveryStateCoordinator)
        singleOf(::DirectConversationStore)
        singleOf(::GroupProtocolPayloadEncoder)
        singleOf(::GroupInvitationManager)
        singleOf(::GroupSecurityManager)
        singleOf(::GroupVerificationPayloadEncoder)
        singleOf(::GroupVerificationCoordinator) {
            bind<GroupVerificationActionRepository>()
        }
        singleOf(::GroupMessageSender)
        singleOf(::GroupInvitationCoordinator)
        singleOf(::IncomingMessageProcessor) {
            bind<IncomingMessageHandler>()
        }

        singleOf(::ChatMessagePacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::ReadReceiptPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::DeliveryReceiptPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupCreatedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupConversationDeletedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupInvitePacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupJoinRequestPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupLeaveRequestPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupInviteDeclinedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupReadyAcknowledgementPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupMemberActivatedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupMemberActivationAcknowledgementPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupMemberRemovedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupVerificationReceiptPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupVerificationSnapshotRequestPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupVerificationSnapshotPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupChatMessagePacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        single<OutboxDeliveryStateListener> {
            ChatOutboxDeliveryStateListener(
                deliveryStateCoordinator = get()
            )
        }

        single { AcceptGroupInvitationUseCase(repository = get()) }
        single { AddGroupMembersUseCase(repository = get()) }
        single { CreateGroupConversationUseCase(repository = get()) }
        single { DeclineGroupInvitationUseCase(repository = get()) }
        single { DeleteConversationUseCase(repository = get()) }
        single { GetGroupLeaveRequirementUseCase(repository = get()) }
        single { GetOrCreateDirectConversationUseCase(repository = get()) }
        single { LeaveGroupUseCase(repository = get()) }
        single { MarkConversationReadUseCase(repository = get()) }
        single { ObserveConversationUseCase(repository = get()) }
        single { ObserveConversationsUseCase(repository = get()) }
        single { ObserveGroupAdministrationUseCase(repository = get()) }
        single { ObserveGroupConversationUseCase(repository = get()) }
        single {
            ObserveGroupVerificationUseCase(
                repository = get(),
                observeContacts = get<ObserveContacts>()
            )
        }
        single { ObserveTypingIndicatorUseCase(repository = get()) }
        single { RetryMessageUseCase(repository = get()) }
        single { PromoteGroupMemberUseCase(repository = get()) }
        single { RemoveGroupMemberUseCase(repository = get()) }
        single { RefreshDeliveryStateUseCase(repository = get()) }
        single { SendMessageUseCase(repository = get()) }
        single { SendGroupMessageUseCase(repository = get()) }
        single { SetTypingIndicatorUseCase(repository = get()) }
        single { SynchronizeGroupVerificationUseCase(repository = get()) }
        single { TransferGroupAdminAndLeaveUseCase(repository = get()) }
        single { VerifyGroupMemberUseCase(repository = get()) }

        single<GroupVerificationRepository> {
            DefaultGroupVerificationRepository(
                groupVerificationDao = get(),
                groupInvitationDao = get(),
                groupSecurityDao = get()
            )
        }

        single<ChatsRepository> {
            DefaultChatsRepository(
                chatDao = get(),
                messageRecipientStateDao = get(),
                directConversationStore = get(),
                deliveryStateCoordinator = get(),
                getContact = get(),
                localPhoneNumberProvider = get(),
                protocolOutbox = get(),
                groupInvitationDao = get(),
                groupSecurityDao = get(),
                groupInvitationCoordinator = get(),
                groupMessageSender = get(),
                identityInvitationService = get(),
                mailboxCapabilityLifecycle = get()
            )
        }

        viewModel {
            ContactsFlowViewModel(
                getOrCreateDirectConversation = get(),
                ensureIdentityExchangeStarted = get<EnsureIdentityExchangeStarted>()
            )
        }

        viewModel {
            OverviewViewModel(
                observeConversations = get(),
                deleteConversationUseCase = get(),
                getGroupLeaveRequirement = get()
            )
        }

        viewModel {
            CreateGroupViewModel(observeContacts = get(), createGroupConversation = get())
        }

        viewModel { parameters ->
            GroupViewModel(
                conversationId = parameters.get(),
                observeConversation = get(),
                observeGroupAdministration = get(),
                observeGroupVerification = get(),
                sendGroupMessage = get(),
                markConversationReadUseCase = get(),
                retryMessageUseCase = get(),
                refreshDeliveryState = get(),
                acceptGroupInvitation = get(),
                declineGroupInvitation = get(),
                observeContacts = get<ObserveContacts>(),
                observeTypingIndicator = get(),
                setTypingIndicator = get()
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
                sendMessageUseCase = get(),
                markConversationReadUseCase = get(),
                retryFailedMessage = get(),
                refreshDeliveryState = get(),
                observeIdentitySetupMode = get<ObserveIdentitySetupMode>(),
                ensureIdentityExchangeStarted = get<EnsureIdentityExchangeStarted>(),
                observeIdentityHandshakeState = get<ObserveIdentityHandshakeState>(),
                observeContact = get<ObserveContact>(),
                observeTypingIndicator = get(),
                setTypingIndicator = get()
            )
        }
    }
