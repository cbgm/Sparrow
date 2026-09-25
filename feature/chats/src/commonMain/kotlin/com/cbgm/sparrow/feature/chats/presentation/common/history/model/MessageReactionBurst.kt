package com.cbgm.sparrow.feature.chats.presentation.common.history.model

import com.cbgm.sparrow.core.ui.component.SparrowOverlayAnchor

internal data class MessageReactionBurst(
    val reactions: List<MessageReactionUi>,
    val anchor: SparrowOverlayAnchor
)
