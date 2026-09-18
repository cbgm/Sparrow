package com.cbgm.sparrow.feature.conversationorchestration.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.AddConversationMembersUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.DeleteConversationGroupUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.DeletePeerConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.GetConversationGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.LeaveConversationGroupUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ObserveConversationQueueAvailabilityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PrepareConversationMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PrepareConversationOpenUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PromoteConversationGroupMemberUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.RemoveConversationGroupMemberUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.TransferConversationGroupAdminAndLeaveUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.conversationorchestration.runtime.DirectIdentityResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.GroupMembershipPacketObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.InvitationResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.MembershipResultObserver
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationOrchestrationModule =
    module {
        singleOf(::GroupMembershipPacketObserver) {
            bind<TypedProtocolPacketHandler>()
        }

        single {
            ConversationFlowHandler(
                startDirectInvitationUseCase = get(),
                acceptDirectInvitation = get(),
                declineDirectInvitation = get(),
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
                confirmGroupMembershipIdentity = get(),
                clearMembershipHandshake = get(),
                markMembershipRemoved = get(),
                getGroupLeaveRequirementUseCase = get(),
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
                blockContact = get(),
                blocklistRepository = get(),
                identitySetupModeRepository = get(),
                conversationPort = get()
            )
        }
        singleOf(::InvitationResultObserver)
        singleOf(::MembershipResultObserver)
        singleOf(::DirectIdentityResultObserver)
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
