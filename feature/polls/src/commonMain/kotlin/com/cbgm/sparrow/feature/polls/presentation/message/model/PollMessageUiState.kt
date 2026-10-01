package com.cbgm.sparrow.feature.polls.presentation.message.model

import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi

data class PollMessageUiState(
    val pollId: String = "",
    val question: String = "",
    val description: String? = null,
    val media: List<MediaItemUi> = emptyList(),
    val options: List<PollOptionUi> = emptyList(),
    val totalVoters: Int = 0,
    val submittedOptionIds: Set<String> = emptySet(),
    val draftOptionIds: Set<String> = emptySet(),
    val allowMultipleSelection: Boolean = false,
    val allowVoteChange: Boolean = true,
    val isClosed: Boolean = false,
    val isExpired: Boolean = false,
    val isVotePending: Boolean = false,
    val expiryLabel: String? = null,
    val canClose: Boolean = false,
    val canInteract: Boolean = false,
    val canSubmitVote: Boolean = false,
    val isChangingVote: Boolean = false
)

data class PollOptionUi(
    val id: String,
    val text: String,
    val voteCount: Int,
    val percentage: Int = 0,
    val isSelected: Boolean = false
)
