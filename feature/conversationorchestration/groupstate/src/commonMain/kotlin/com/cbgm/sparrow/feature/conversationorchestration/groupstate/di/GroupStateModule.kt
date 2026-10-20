package com.cbgm.sparrow.feature.conversationorchestration.groupstate.di

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource.GroupDescriptionDataSource
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource.GroupTitleDataSource
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.packet.GroupAvatarPacketCodecImpl
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.packet.GroupDescriptionPacketCodecImpl
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.packet.GroupPinPacketCodecImpl
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.packet.GroupTitlePacketCodecImpl
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.repository.GroupAvatarRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.repository.GroupDescriptionRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.repository.GroupPinRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.repository.GroupTitleRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupAvatarPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupDescriptionPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupPinPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupTitlePacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.ClearGroupStateUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupAvatarPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupDescriptionPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupPinPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupTitlePacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindGroupExpenseMembersUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupPaymentTransferUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SendCurrentGroupAvatarUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SendCurrentGroupDescriptionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SendCurrentGroupTitleUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense.ActivateGroupExpensesWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense.CreateGroupExpenseWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense.DeleteGroupExpenseWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense.EditGroupExpenseWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense.SendCurrentGroupExpensesWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata.ChangeGroupAvatarWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata.ChangeGroupDescriptionWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata.ChangeGroupTitleWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata.PublishGroupAvatarWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata.RemoveGroupAvatarWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata.SendCurrentGroupMetadataWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.payment.SendGroupPaymentWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin.CloseGroupExpensesWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin.PinGroupMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin.SendCurrentGroupPinWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin.UnpinGroupMessageWorkflow
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationGroupStateModule =
    module {
        registerData()
        registerUseCases()
        registerInternalWorkflows()
        registerPublicWorkflows()
    }

private fun Module.registerData() {
    singleOf(::GroupAvatarDataSource)
    singleOf(::GroupDescriptionDataSource)
    singleOf(::GroupPinDataSource)
    singleOf(::GroupTitleDataSource)
    singleOf(::GroupAvatarPacketCodecImpl) { bind<GroupAvatarPacketCodec>() }
    singleOf(::GroupAvatarRepositoryImpl) { bind<GroupAvatarRepository>() }
    singleOf(::GroupDescriptionPacketCodecImpl) { bind<GroupDescriptionPacketCodec>() }
    singleOf(::GroupDescriptionRepositoryImpl) { bind<GroupDescriptionRepository>() }
    singleOf(::GroupPinPacketCodecImpl) { bind<GroupPinPacketCodec>() }
    singleOf(::GroupPinRepositoryImpl) { bind<GroupPinRepository>() }
    singleOf(::GroupTitlePacketCodecImpl) { bind<GroupTitlePacketCodec>() }
    singleOf(::GroupTitleRepositoryImpl) { bind<GroupTitleRepository>() }
}

private fun Module.registerUseCases() {
    singleOf(::ClearGroupStateUseCase)
    singleOf(::CreateGroupAvatarPacketsUseCase)
    singleOf(::CreateGroupDescriptionPacketsUseCase)
    singleOf(::CreateGroupPinPacketsUseCase)
    singleOf(::CreateGroupTitlePacketsUseCase)
    singleOf(::RequireGroupPaymentTransferUseCase)
    singleOf(::FindGroupExpenseMembersUseCase)
    singleOf(::RequireGroupAdminContextUseCase)
    singleOf(::SendCurrentGroupAvatarUseCase)
    singleOf(::SendCurrentGroupDescriptionUseCase)
    singleOf(::SendCurrentGroupTitleUseCase)
}

private fun Module.registerInternalWorkflows() {
    singleOf(::PublishGroupAvatarWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::SendCurrentGroupMetadataWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::SendCurrentGroupPinWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
}

/** Public workflows are reachable only through ConversationOrchestrator.execute. */
private fun Module.registerPublicWorkflows() {
    singleOf(::ActivateGroupExpensesWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ChangeGroupAvatarWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ChangeGroupDescriptionWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ChangeGroupTitleWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::CloseGroupExpensesWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::SendGroupPaymentWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::CreateGroupExpenseWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::EditGroupExpenseWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::DeleteGroupExpenseWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::SendCurrentGroupExpensesWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::PinGroupMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RemoveGroupAvatarWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::UnpinGroupMessageWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
}
