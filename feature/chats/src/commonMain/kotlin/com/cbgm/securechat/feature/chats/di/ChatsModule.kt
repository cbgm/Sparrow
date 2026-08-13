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
import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationGateway
import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationRepository
import com.cbgm.securechat.feature.chats.domain.usecase.AcceptGroupInvitation
import com.cbgm.securechat.feature.chats.domain.usecase.AddGroupMembers
import com.cbgm.securechat.feature.chats.domain.usecase.CreateGroupConversation
import com.cbgm.securechat.feature.chats.domain.usecase.DeclineGroupInvitation
import com.cbgm.securechat.feature.chats.domain.usecase.DeleteConversation
import com.cbgm.securechat.feature.chats.domain.usecase.GetGroupLeaveRequirement
import com.cbgm.securechat.feature.chats.domain.usecase.GetOrCreateDirectConversation
import com.cbgm.securechat.feature.chats.domain.usecase.LeaveGroup
import com.cbgm.securechat.feature.chats.domain.usecase.MarkConversationRead
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversation
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversations
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupAdministration
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupConversation
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupVerification
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveTypingIndicator
import com.cbgm.securechat.feature.chats.domain.usecase.PromoteGroupMember
import com.cbgm.securechat.feature.chats.domain.usecase.RefreshDeliveryState
import com.cbgm.securechat.feature.chats.domain.usecase.RemoveGroupMember
import com.cbgm.securechat.feature.chats.domain.usecase.RetryMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SendGroupMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SendMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SetTypingIndicator
import com.cbgm.securechat.feature.chats.domain.usecase.SynchronizeGroupVerification
import com.cbgm.securechat.feature.chats.domain.usecase.TransferGroupAdminAndLeave
import com.cbgm.securechat.feature.chats.domain.usecase.VerifyGroupMember
import com.cbgm.securechat.feature.chats.presentation.screen.chat.ChatViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.chat.GroupChatViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.create.CreateGroupViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.details.GroupMemberQrVerificationViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.details.GroupVerificationViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.overview.ChatsViewModel
import com.cbgm.securechat.feature.contacts.domain.usecase.EnsureIdentityExchangeStarted
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactSafetyNumber
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveIdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveIdentitySetupMode
import com.cbgm.securechat.presentation.screen.ContactsFlowViewModel
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
            bind<GroupVerificationGateway>()
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

        single { AcceptGroupInvitation(repository = get()) }
        single { AddGroupMembers(repository = get()) }
        single { CreateGroupConversation(repository = get()) }
        single { DeclineGroupInvitation(repository = get()) }
        single { DeleteConversation(repository = get()) }
        single { GetGroupLeaveRequirement(repository = get()) }
        single { GetOrCreateDirectConversation(repository = get()) }
        single { LeaveGroup(repository = get()) }
        single { MarkConversationRead(repository = get()) }
        single { ObserveConversation(repository = get()) }
        single { ObserveConversations(repository = get()) }
        single { ObserveGroupAdministration(repository = get()) }
        single { ObserveGroupConversation(repository = get()) }
        single {
            ObserveGroupVerification(
                repository = get(),
                observeContacts = get<ObserveContacts>()
            )
        }
        single { ObserveTypingIndicator(gateway = get()) }
        single { RetryMessage(repository = get()) }
        single { PromoteGroupMember(repository = get()) }
        single { RemoveGroupMember(repository = get()) }
        single { RefreshDeliveryState(repository = get()) }
        single { SendMessage(repository = get()) }
        single { SendGroupMessage(repository = get()) }
        single { SetTypingIndicator(gateway = get()) }
        single { SynchronizeGroupVerification(gateway = get()) }
        single { TransferGroupAdminAndLeave(repository = get()) }
        single { VerifyGroupMember(gateway = get()) }

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
            ChatsViewModel(
                observeConversations = get(),
                deleteConversationUseCase = get(),
                getGroupLeaveRequirement = get()
            )
        }

        viewModel {
            CreateGroupViewModel(observeContacts = get(), createGroupConversation = get())
        }

        viewModel { parameters ->
            GroupChatViewModel(
                conversationId = parameters.get(),
                observeConversation = get(),
                observeGroupAdministration = get(),
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
            ChatViewModel(
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
