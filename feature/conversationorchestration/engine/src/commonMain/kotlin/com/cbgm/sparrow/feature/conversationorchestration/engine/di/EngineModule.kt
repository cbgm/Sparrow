package com.cbgm.sparrow.feature.conversationorchestration.engine.di

import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.orchestrator.DefaultConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationEngineModule =
    module {
        singleOf(::WorkflowLocks)
        single { WorkflowRegistry { getAll<WorkflowStateMachine<*, *, *>>() } }
        singleOf(::WorkflowEngine)
        singleOf(::DefaultConversationOrchestrator) { bind<ConversationOrchestrator>() }
    }
