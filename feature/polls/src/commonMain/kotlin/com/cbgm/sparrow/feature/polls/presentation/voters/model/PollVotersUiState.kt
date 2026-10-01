package com.cbgm.sparrow.feature.polls.presentation.voters.model

import com.cbgm.sparrow.feature.polls.presentation.model.PollVoterUi

data class PollVotersUiState(
    val pollId: String,
    val question: String,
    val totalVoters: Int,
    val sections: List<PollVoterSectionUi>
)

data class PollVoterSectionUi(
    val optionId: String,
    val optionText: String,
    val voteCount: Int,
    val percentage: Int,
    val voters: List<PollVoterUi>
)
