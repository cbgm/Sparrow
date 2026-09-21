package com.cbgm.sparrow.feature.chats.presentation.common.history.model

import com.cbgm.sparrow.core.ui.component.SparrowOverlayAnchor

internal data class MessageContextAnchor(
    val messageId: String,
    val overlayAnchor: SparrowOverlayAnchor,
    val isMine: Boolean
)
