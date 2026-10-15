package com.cbgm.sparrow.feature.conversationorchestration.engine.machine

/**
 * A command that only orchestration itself submits: runtime observers and workflows that start another
 * workflow. Each module lists its internal commands in one sealed interface extending this marker.
 * Commands Chats may submit are [com.cbgm.sparrow.feature.conversationorchestration.api.command.ConversationCommand]s.
 */
interface WorkflowCommand
