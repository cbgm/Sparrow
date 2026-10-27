package com.cbgm.sparrow.feature.conversationorchestration.incoming.di

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.datasource.ParkedMessageOperationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.packet.DirectMessagePacketReaderImpl
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.repository.ParkedMessageOperationRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.dispatch.ControlPacketDispatcher
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.dispatch.GroupPacketDispatcher
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.packet.DirectMessagePacketReader
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository.ParkedMessageOperationRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.AuthorizeIncomingExpensesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.AuthorizeRemoteMessageOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.CheckDirectPeerAuthorizedUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.CheckGroupPacketAcceptedUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.DeleteReplacedExpenseReceiptsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.EvaluateGroupInvitationIdentityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.FindOperationTargetUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.InspectIncomingGroupOfferUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.MergeIncomingIdentityPeersUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.ResolveContactInvitationDisplayNameUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.ResolveControlConversationIdUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.ResolveIncomingIdentityPeerUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.ResolveSenderDisplayNameUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.SendDeliveryReceiptUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.StageIncomingIdentityReviewUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.StoreIncomingDirectAttachmentsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.StoreUnreadableMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.UpdateIncomingSenderProfileUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.VerifyIncomingContactInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.direct.ReceiveDirectMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.group.ReceiveGroupMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity.CompleteManualIdentityWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity.ReceiveAuthorizationRevokedWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity.ReceiveContactInviteAcceptedWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity.ReceiveContactInviteDeclinedWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity.ReceiveContactInviteWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity.ReceiveContactReadyWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity.ReceiveIdentityAcknowledgementWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity.ReceiveManualIdentityWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupActivationAcknowledgementWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupDeletionWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupInviteDeclineWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupInviteReceiptWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupInviteWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupJoinRequestWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupLeaveRequestWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupMemberActivationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupMemberRemovalWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupReadyAcknowledgementWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership.ReceiveGroupWelcomeWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata.ReceiveGroupAvatarWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata.ReceiveGroupDescriptionWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata.ReceiveGroupPinWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata.ReceiveGroupTitleWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation.ApplyParkedMessageOperationsWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation.ApplyRemoteMessageOperationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.routing.RouteIncomingPacketWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.verification.ReceiveVerificationReceiptWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.verification.ReceiveVerificationSnapshotRequestWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.verification.ReceiveVerificationSnapshotWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.incoming.runtime.IncomingEnvelopeProcessorImpl
import com.cbgm.sparrow.feature.conversationorchestration.incoming.runtime.IncomingTransportMessageHandler
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.protocol.handler.IncomingMessageHandler
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationIncomingModule =
    module {
        registerData()
        registerUseCases()
        registerInternalWorkflows()
        singleOf(::IncomingTransportMessageHandler) { bind<IncomingMessageHandler>() }
        singleOf(::IncomingEnvelopeProcessorImpl) { bind<IncomingEnvelopeProcessor>() }
        singleOf(::ControlPacketDispatcher)
        singleOf(::GroupPacketDispatcher)
    }

private fun Module.registerData() {
    singleOf(::DirectMessagePacketReaderImpl) { bind<DirectMessagePacketReader>() }
    singleOf(::ParkedMessageOperationDataSource)
    singleOf(::ParkedMessageOperationRepositoryImpl) { bind<ParkedMessageOperationRepository>() }
}

private fun Module.registerUseCases() {
    singleOf(::AuthorizeIncomingExpensesUseCase)
    singleOf(::AuthorizeRemoteMessageOperationUseCase)
    singleOf(::CheckDirectPeerAuthorizedUseCase)
    singleOf(::CheckGroupPacketAcceptedUseCase)
    singleOf(::DeleteReplacedExpenseReceiptsUseCase)
    singleOf(::EvaluateGroupInvitationIdentityUseCase)
    singleOf(::FindOperationTargetUseCase)
    singleOf(::InspectIncomingGroupOfferUseCase)
    singleOf(::MergeIncomingIdentityPeersUseCase)
    singleOf(::ResolveContactInvitationDisplayNameUseCase)
    singleOf(::ResolveControlConversationIdUseCase)
    singleOf(::ResolveIncomingIdentityPeerUseCase)
    singleOf(::ResolveSenderDisplayNameUseCase)
    singleOf(::SendDeliveryReceiptUseCase)
    singleOf(::StageIncomingIdentityReviewUseCase)
    singleOf(::StoreIncomingDirectAttachmentsUseCase)
    singleOf(::StoreUnreadableMessageUseCase)
    singleOf(::UpdateIncomingSenderProfileUseCase)
    singleOf(::VerifyIncomingContactInvitationUseCase)
}

private fun Module.registerInternalWorkflows() {
    singleOf(::ApplyRemoteMessageOperationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ApplyParkedMessageOperationsWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::CompleteManualIdentityWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveAuthorizationRevokedWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveContactInviteAcceptedWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveContactInviteDeclinedWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveContactInviteWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveContactReadyWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveDirectMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupActivationAcknowledgementWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupAvatarWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupDeletionWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupDescriptionWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupInviteDeclineWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupInviteReceiptWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupInviteWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupJoinRequestWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupLeaveRequestWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupMemberActivationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupMemberRemovalWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupPinWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupReadyAcknowledgementWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupTitleWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveGroupWelcomeWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveIdentityAcknowledgementWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveManualIdentityWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveVerificationReceiptWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveVerificationSnapshotRequestWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReceiveVerificationSnapshotWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RouteIncomingPacketWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
}
