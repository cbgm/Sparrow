package com.cbgm.sparrow.feature.chats.presentation.forwarding.model

import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget

sealed interface ForwardingSelectionEffect {
    data class TargetSelected(
        val target: ForwardingTarget
    ) : ForwardingSelectionEffect
}
