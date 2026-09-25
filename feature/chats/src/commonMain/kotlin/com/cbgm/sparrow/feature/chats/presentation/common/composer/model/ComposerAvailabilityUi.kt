package com.cbgm.sparrow.feature.chats.presentation.common.composer.model

/** Presentation-only snapshot of composer actions allowed by the domain policy. */
data class ComposerAvailabilityUi(
    val isInputEnabled: Boolean,
    val isSendEnabled: Boolean,
    val canAddAttachment: Boolean
)
