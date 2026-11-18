package com.cbgm.sparrow.feature.chats.presentation.common.header.model

import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarRing

/** Header-specific presentation data; no conversation or domain models are passed to the UI. */
data class HeaderUiModel(
    val title: String,
    val avatarId: String,
    val avatarKind: HeaderAvatarKind,
    val subtitle: String? = null,
    val ring: AvatarRing = AvatarRing.NONE
)

enum class HeaderAvatarKind { USER, GROUP }
