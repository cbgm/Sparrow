package com.cbgm.sparrow.feature.conversationorchestration.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.AddConversationMembersUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.DeleteConversationGroupUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.DeletePeerConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.GetConversationGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.GroupVerificationInputsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.LeaveConversationGroupUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ObserveConversationQueueAvailabilityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PrepareConversationMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PrepareConversationOpenUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PromoteConversationGroupMemberUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.RemoveConversationGroupMemberUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ResolveIncomingIdentityPeerUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ResolveSigningIdentityContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.TransferConversationGroupAdminAndLeaveUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.conversationorchestration.runtime.ContactBlockObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.GroupMembershipPacketObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.IdentityExchangePacketObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.IdentityResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.InvitationResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.MembershipResultObserver
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
        singleOf(::IdentityResultObserver)
        singleOf(::ContactBlockObserver)
        singleOf(::ObserveConversationQueueAvailabilityUseCase)
        singleOf(::PrepareConversationMessageUseCase)
        singleOf(::PrepareConversationOpenUseCase)
        singleOf(::AddConversationMembersUseCase)
        singleOf(::GetConversationGroupLeaveRequirementUseCase)
        singleOf(::PromoteConversationGroupMemberUseCase)
        singleOf(::RemoveConversationGroupMemberUseCase)
        singleOf(::TransferConversationGroupAdminAndLeaveUseCase)
        singleOf(::LeaveConversationGroupUseCase)
        singleOf(::DeleteConversationGroupUseCase)
        singleOf(::DeletePeerConversationUseCase)
    }
