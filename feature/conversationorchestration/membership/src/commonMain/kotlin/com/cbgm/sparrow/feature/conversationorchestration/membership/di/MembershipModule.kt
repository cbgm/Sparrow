package com.cbgm.sparrow.feature.conversationorchestration.membership.di

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource.GroupConversationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource.GroupIncomingConversationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource.GroupLocalCleanupDataSource
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource.GroupVerificationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.packet.GroupVerificationPacketCodecImpl
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.packet.GroupVerificationPayloadEncoder
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.repository.GroupConversationRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.repository.GroupIncomingConversationRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.repository.GroupVerificationRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.packet.GroupVerificationPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupIncomingConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.AcceptParticipantVerificationReceiptUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.AcceptVerificationSnapshotRequestUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ApplyIncomingGroupRemovalUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ApplyVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.BroadcastGroupVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.CheckGroupOwnershipUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ConfirmGroupMemberIdentityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ConfirmParticipantVerificationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.DiscardDeclinedGroupConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindGroupMemberRemovalProgressUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindMemberDisplayNameUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.GetTrustedGroupAuthorityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.GroupVerificationInputsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.LoadGroupVerificationSignerUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.LoadMutualMemberIdentityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RecordGroupInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RefreshOwnedGroupVerificationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RequestGroupVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RequireMemberPhoneNumberUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ResolveGroupVerificationRoleUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ResolveSigningIdentityContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.SendAdminVerificationReceiptUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.SendGroupVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration.AddGroupMembersWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration.CreateGroupWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration.DeleteGroupWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration.LeaveGroupWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration.PromoteGroupMemberWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration.RemoveGroupMemberWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration.TransferGroupAdminAndLeaveWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.join.HandleGroupInvitationResultWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.join.HandleMembershipResultWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.join.RecoverAcceptedGroupInvitationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.join.ResumeApprovedGroupJoinWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification.RefreshOwnedGroupVerificationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification.SynchronizeGroupVerificationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification.VerifyGroupMemberWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.membership.runtime.MembershipResultObserver
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationMembershipModule =
    module {
        registerData()
        registerUseCases()
        registerInternalWorkflows()
        registerPublicWorkflows()
        singleOf(::GroupVerificationPayloadEncoder)
        singleOf(::MembershipResultObserver)
    }

private fun Module.registerData() {
    singleOf(::GroupConversationDataSource)
    singleOf(::GroupIncomingConversationDataSource)
    singleOf(::GroupLocalCleanupDataSource)
    singleOf(::GroupVerificationDataSource)
    singleOf(::GroupConversationRepositoryImpl) { bind<GroupConversationRepository>() }
    singleOf(::GroupIncomingConversationRepositoryImpl) { bind<GroupIncomingConversationRepository>() }
    singleOf(::GroupVerificationPacketCodecImpl) { bind<GroupVerificationPacketCodec>() }
    singleOf(::GroupVerificationRepositoryImpl) { bind<GroupVerificationRepository>() }
}

private fun Module.registerUseCases() {
    singleOf(::ConfirmGroupMemberIdentityUseCase)
    singleOf(::RequireMemberPhoneNumberUseCase)
    singleOf(::AcceptParticipantVerificationReceiptUseCase)
    singleOf(::AcceptVerificationSnapshotRequestUseCase)
    singleOf(::ApplyIncomingGroupRemovalUseCase)
    singleOf(::ApplyVerificationSnapshotUseCase)
    singleOf(::BroadcastGroupVerificationSnapshotUseCase)
    singleOf(::CheckGroupOwnershipUseCase)
    singleOf(::ConfirmParticipantVerificationUseCase)
    singleOf(::FindGroupMemberRemovalProgressUseCase)
    singleOf(::FindMemberDisplayNameUseCase)
    singleOf(::GroupVerificationInputsUseCase)
    singleOf(::LoadGroupVerificationSignerUseCase)
    singleOf(::RecordGroupInvitationUseCase)
    singleOf(::RefreshOwnedGroupVerificationUseCase)
    singleOf(::RequestGroupVerificationSnapshotUseCase)
    singleOf(::ResolveSigningIdentityContactUseCase)
    singleOf(::SendAdminVerificationReceiptUseCase)
    singleOf(::SendGroupVerificationSnapshotUseCase)
    singleOf(::DiscardDeclinedGroupConversationUseCase)
    singleOf(::LoadMutualMemberIdentityUseCase)
    singleOf(::GetTrustedGroupAuthorityUseCase)
    singleOf(::ResolveGroupVerificationRoleUseCase)
}

private fun Module.registerInternalWorkflows() {
    singleOf(::HandleGroupInvitationResultWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::HandleMembershipResultWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RecoverAcceptedGroupInvitationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ResumeApprovedGroupJoinWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RefreshOwnedGroupVerificationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
}

/** Public workflows are reachable only through ConversationOrchestrator.execute. */
private fun Module.registerPublicWorkflows() {
    singleOf(::AddGroupMembersWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::CreateGroupWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::DeleteGroupWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::LeaveGroupWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::PromoteGroupMemberWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RemoveGroupMemberWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::SynchronizeGroupVerificationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::TransferGroupAdminAndLeaveWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::VerifyGroupMemberWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
}
