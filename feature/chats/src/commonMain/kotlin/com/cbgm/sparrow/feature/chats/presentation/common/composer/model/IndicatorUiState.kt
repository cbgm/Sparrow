package com.cbgm.sparrow.feature.chats.presentation.common.composer.model

enum class IndicatorUiType { NONE, TYPING, VOICE }

data class IndicatorUiState(
    val type: IndicatorUiType = IndicatorUiType.NONE,
    val displayName: String = ""
)
