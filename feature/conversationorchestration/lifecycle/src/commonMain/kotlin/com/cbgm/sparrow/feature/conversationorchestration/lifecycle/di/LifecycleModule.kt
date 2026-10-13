package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.di

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.ApplyPeerProfilePictureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.CheckIncomingInvitationAllowedUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.RecordIncomingDirectInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.RecordOutgoingDirectInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct.DeleteDirectConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct.OpenDirectConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct.ReconnectDirectConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct.RequestReauthorizationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct.RevokePeerExchangeWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct.StartDirectInvitationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.recovery.AcceptApprovedIdentityChangeWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.recovery.RecoverAcceptedDirectInvitationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.recovery.RecoverAuthorizedConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.recovery.StartRecoveryInvitationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result.HandleContactBlockedWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result.HandleDirectInvitationResultWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result.HandleIdentityResultWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result.RecordIncomingDirectInvitationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.ApprovedIdentityReconnectionObserver
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.ContactBlockObserver
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.IdentityResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.InvitationResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.InvitationTransportFailureHandler
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.MessagingTransportResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.WorkflowDirectReauthorization
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.port.DirectReauthorizationPort
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationLifecycleModule =
    module {
        registerUseCases()
        registerInternalWorkflows()
        registerPublicWorkflows()
        singleOf(::WorkflowDirectReauthorization) { bind<DirectReauthorizationPort>() }
        singleOf(::ApprovedIdentityReconnectionObserver)
        singleOf(::ContactBlockObserver)
        singleOf(::IdentityResultObserver)
        singleOf(::InvitationResultObserver)
        singleOf(::InvitationTransportFailureHandler)
        singleOf(::MessagingTransportResultObserver)
    }

private fun Module.registerUseCases() {
    singleOf(::ApplyPeerProfilePictureUseCase)
    singleOf(::CheckIncomingInvitationAllowedUseCase)
    singleOf(::RecordIncomingDirectInvitationUseCase)
    singleOf(::RecordOutgoingDirectInvitationUseCase)
}

private fun Module.registerInternalWorkflows() {
    singleOf(::AcceptApprovedIdentityChangeWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::HandleContactBlockedWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::HandleDirectInvitationResultWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::HandleIdentityResultWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RecordIncomingDirectInvitationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RecoverAcceptedDirectInvitationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RecoverAuthorizedConversationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RequestReauthorizationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::RevokePeerExchangeWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::StartDirectInvitationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::StartRecoveryInvitationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
}

/** Public workflows are reachable only through ConversationOrchestrator.execute. */
private fun Module.registerPublicWorkflows() {
    singleOf(::DeleteDirectConversationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::OpenDirectConversationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
    singleOf(::ReconnectDirectConversationWorkflow) { bind<WorkflowStateMachine<*, *, *>>() }
}
