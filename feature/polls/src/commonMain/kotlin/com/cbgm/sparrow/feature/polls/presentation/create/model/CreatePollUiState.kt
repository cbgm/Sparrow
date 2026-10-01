package com.cbgm.sparrow.feature.polls.presentation.create.model

import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi

data class CreatePollUiState(
    val question: String = "",
    val description: String = "",
    val options: List<PollOptionEditorUi> = emptyList(),
    val media: List<VisualMediaSelectionUi> = emptyList(),
    val expiryEnabled: Boolean = false,
    val expiryDate: String = "",
    val expiryTime: String = "",
    val expiresAtEpochMilliseconds: Long? = null,
    val expiryInvalid: Boolean = false,
    val allowMultipleSelection: Boolean = false,
    val allowVoteChange: Boolean = true,
    val isAnonymous: Boolean = false,
    val canCreate: Boolean = false
)

data class PollOptionEditorUi(
    val id: String,
    val text: String = ""
)
