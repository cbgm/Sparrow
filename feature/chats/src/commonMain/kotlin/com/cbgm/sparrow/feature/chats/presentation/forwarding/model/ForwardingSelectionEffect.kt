package com.cbgm.sparrow.feature.chats.presentation.forwarding.model

import com.cbgm.sparrow.feature.conversationorchestration.api.command.ForwardTarget

sealed interface ForwardingSelectionEffect {
    data class TargetSelected(
        val target: ForwardTarget
    ) : ForwardingSelectionEffect
}
