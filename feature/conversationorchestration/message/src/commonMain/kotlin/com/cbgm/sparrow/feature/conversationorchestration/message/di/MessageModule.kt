package com.cbgm.sparrow.feature.conversationorchestration.message.di

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.ConversationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.DirectMessageDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.GroupMessageDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.MessageReactionDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.data.packet.DirectMessagePacketFactoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.message.data.packet.GroupMessagePlaintextCodecImpl
import com.cbgm.sparrow.feature.conversationorchestration.message.data.repository.ConversationUnseenCountRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.message.data.repository.DirectConversationRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.message.data.repository.DirectMessageRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.message.data.repository.GroupMessageRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.message.data.repository.MessageReactionRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.DirectMessagePacketFactory
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.GroupMessagePlaintextCodec
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ConversationUnseenCountRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.MessageReactionRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.AcknowledgeIncomingMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckGroupMessageAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckMessageEditableUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckPollClosePermissionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CreateDirectMessagePacketUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardExpiredWaitingDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardGroupMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupPayloadUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDeletableDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDeletableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectAuthorizationBlockUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindEditableDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindEditableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupMessagesToFlushUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindRetryableDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindRetryableGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindUndeliveredGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.InspectDirectInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.LoadDirectRecipientUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordPacketSendFailureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordRetryRequestedUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecoverInterruptedDirectSendsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.ReleaseWaitingDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireGroupConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireMessageWithoutPaymentUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RestoreWaitingDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SaveQueuedGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendDirectOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendReadReceiptUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.StoreOutgoingDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.StoreOutgoingGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.ToggleLocalReactionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.DeleteDirectMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.DeliverDirectMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.EditDirectMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.MarkDirectConversationReadWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.RetryDirectMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.SendDirectIndicatorWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.SendDirectMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.ToggleDirectReactionWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization.ActivateAuthorizedConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization.DiscardWaitingDirectMessagesWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization.ReleaseWaitingDirectMessagesWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.forward.ForwardMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.CloseGroupPollWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.DeleteGroupMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.EditGroupMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.FlushQueuedGroupMessagesWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.MarkGroupConversationReadWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.RetryGroupMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.SendGroupIndicatorWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.SendGroupMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.ToggleGroupReactionWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group.VoteGroupPollWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.runtime.PendingAuthorizationCleanupRunner
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationMessageModule =
    module {
        registerData()
        registerUseCases()
        registerInternalWorkflows()
        registerPublicWorkflows()
        singleOf(::PendingAuthorizationCleanupRunner)
    }

private fun Module.registerData() {
    singleOf(::ConversationDataSource)
    singleOf(::DirectMessageDataSource)
    singleOf(::GroupMessageDataSource)
    singleOf(::MessageReactionDataSource)
    singleOf(::ConversationUnseenCountRepositoryImpl) { bind<ConversationUnseenCountRepository>() }
    singleOf(::DirectConversationRepositoryImpl) { bind<DirectConversationRepository>() }
    singleOf(::DirectMessageRepositoryImpl) { bind<DirectMessageRepository>() }
    singleOf(::GroupMessageRepositoryImpl) { bind<GroupMessageRepository>() }
    singleOf(::MessageReactionRepositoryImpl) { bind<MessageReactionRepository>() }
    singleOf(::DirectMessagePacketFactoryImpl) { bind<DirectMessagePacketFactory>() }
    singleOf(::GroupMessagePlaintextCodecImpl) { bind<GroupMessagePlaintextCodec>() }
}

private fun Module.registerUseCases() {
    singleOf(::RequireMessageWithoutPaymentUseCase)
    singleOf(::CheckGroupMessageAccessUseCase)
    singleOf(::DiscardDirectMessagesUseCase)
    singleOf(::DiscardGroupMessagesUseCase)
    singleOf(::DeleteLocalExpenseReceiptsUseCase)
    singleOf(::EncryptGroupMessageUseCase)
    singleOf(::EncryptGroupOperationUseCase)
    singleOf(::EncryptGroupPayloadUseCase)
    singleOf(::EnqueueGroupPacketsUseCase)
    singleOf(::FindDirectAuthorizationBlockUseCase)
    singleOf(::FindGroupMessageUseCase)
    singleOf(::FindGroupPollUseCase)
    singleOf(::FindGroupRecipientsUseCase)
    singleOf(::InspectDirectInvitationUseCase)
    singleOf(::LoadDirectRecipientUseCase)
    singleOf(::RecordPacketSendFailureUseCase)
    singleOf(::RecordRetryRequestedUseCase)
    singleOf(::RecoverInterruptedDirectSendsUseCase)
    singleOf(::RequireDirectChatAuthorizationUseCase)
    singleOf(::SendDirectOperationUseCase)
    singleOf(::SendReadReceiptUseCase)
    singleOf(::StoreOutgoingDirectMessageUseCase)
    singleOf(::StoreOutgoingGroupMessageUseCase)
    singleOf(::AcknowledgeIncomingMessageUseCase)
    singleOf(::CheckMessageEditableUseCase)
    singleOf(::CheckPollClosePermissionUseCase)
    singleOf(::CreateDirectMessagePacketUseCase)
    singleOf(::DiscardExpiredWaitingDirectMessagesUseCase)
    singleOf(::FindDeletableDirectMessageUseCase)
    singleOf(::FindDeletableGroupMessageUseCase)
    singleOf(::FindDirectMessageUseCase)
    singleOf(::FindEditableDirectMessageUseCase)
    singleOf(::FindEditableGroupMessageUseCase)
    singleOf(::FindGroupMessagesToFlushUseCase)
    singleOf(::FindGroupPollMessageUseCase)
    singleOf(::FindRetryableDirectMessageUseCase)
    singleOf(::FindRetryableGroupRecipientsUseCase)
    singleOf(::FindUndeliveredGroupRecipientsUseCase)
    singleOf(::ReleaseWaitingDirectMessageUseCase)
    singleOf(::RequireGroupConversationUseCase)
    singleOf(::RestoreWaitingDirectMessageUseCase)
    singleOf(::SaveQueuedGroupRecipientsUseCase)
    singleOf(::ToggleLocalReactionUseCase)
}

private fun Module.registerInternalWorkflows() {
    singleOf(::DeliverDirectMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReleaseWaitingDirectMessagesWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::DiscardWaitingDirectMessagesWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ActivateAuthorizedConversationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::FlushQueuedGroupMessagesWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
}

/** Every public workflow is reachable only through ConversationOrchestrator.execute. */
private fun Module.registerPublicWorkflows() {
    singleOf(::SendDirectMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::EditDirectMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::DeleteDirectMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ToggleDirectReactionWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RetryDirectMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::MarkDirectConversationReadWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::SendDirectIndicatorWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::SendGroupMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::EditGroupMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::DeleteGroupMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ToggleGroupReactionWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::VoteGroupPollWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::CloseGroupPollWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RetryGroupMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::MarkGroupConversationReadWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::SendGroupIndicatorWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ForwardMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
}
