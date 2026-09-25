package com.cbgm.sparrow.feature.chats.presentation.forwarding.model

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget

data class ForwardingTargetUi(
    val id: String,
    val displayName: String,
    val avatarTarget: AvatarTarget,
    val target: ForwardingTarget
)
