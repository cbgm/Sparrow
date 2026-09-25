package com.cbgm.sparrow.feature.chats.presentation.common.header.model

/** Header-specific presentation data; no conversation or domain models are passed to the UI. */
data class HeaderUiModel(
    val title: String,
    val avatarId: String,
    val avatarKind: HeaderAvatarKind,
    val subtitle: String? = null
)

enum class HeaderAvatarKind { USER, GROUP }
