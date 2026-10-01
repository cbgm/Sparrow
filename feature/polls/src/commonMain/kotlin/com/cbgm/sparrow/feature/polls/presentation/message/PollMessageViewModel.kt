package com.cbgm.sparrow.feature.polls.presentation.message

import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollMessageUiState
import com.cbgm.sparrow.feature.polls.presentation.voters.model.PollVoterSectionUi
import com.cbgm.sparrow.feature.polls.presentation.voters.model.PollVotersUiState
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_MESSAGE_MEDIA_PREVIEW
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_VOTER_PREVIEW
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PollMessageViewModel(
    initialState: PollMessageUiState
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(initialState.derived())
    val uiState: StateFlow<PollMessageUiState> = _uiState.asStateFlow()

    fun onOptionClick(optionId: String) {
        _uiState.update { state ->
            if (!state.canInteract || state.options.none { it.id == optionId }) return@update state

            val updated =
                if (state.allowMultipleSelection) {
                    if (optionId in state.draftOptionIds) {
                        state.draftOptionIds - optionId
                    } else {
                        state.draftOptionIds + optionId
                    }
                } else if (state.draftOptionIds == setOf(optionId)) {
                    emptySet()
                } else {
                    setOf(optionId)
                }

            state.copy(draftOptionIds = updated).derived()
        }
    }

    fun submitVote(onSubmit: (Set<String>) -> Unit) {
        val state = _uiState.value
        if (!state.canSubmitVote) return
        onSubmit(state.draftOptionIds)
    }

    fun openVoters() {
        _uiState.update { state ->
            if (!state.canShowVotes) return@update state
            state.copy(votersOverlay = state.toVotersUiState())
        }
    }

    fun dismissVoters() {
        _uiState.update { state ->
            state.copy(votersOverlay = null)
        }
    }

    private fun PollMessageUiState.derived(): PollMessageUiState {
        val locked = isClosed || isExpired || (!allowVoteChange && submittedOptionIds.isNotEmpty())
        val interactionEnabled = !locked && !isVotePending
        val optionIds = options.mapTo(mutableSetOf()) { it.id }
        val validDraft = draftOptionIds.intersect(optionIds)
        val normalizedOptions =
            options.map { option ->
                val percentage =
                    if (totalVoters <= 0) {
                        0
                    } else {
                        ((option.voteCount.toDouble() / totalVoters.toDouble()) * 100.0)
                            .toInt()
                            .coerceIn(0, 100)
                    }
                val visibleVoters = if (isAnonymous) emptyList() else option.voters
                option.copy(
                    voters = visibleVoters,
                    voterPreview = visibleVoters.take(MAX_VOTER_PREVIEW),
                    percentage = percentage,
                    isSelected = option.id in validDraft
                )
            }
        val showVotes = !isAnonymous && normalizedOptions.any { it.voters.isNotEmpty() }
        val visibleMedia = media.take(MAX_MESSAGE_MEDIA_PREVIEW)
        val normalizedState =
            copy(
                mediaPreview = visibleMedia,
                remainingMediaCount = (media.size - visibleMedia.size).coerceAtLeast(0),
                options = normalizedOptions,
                draftOptionIds = validDraft,
                canInteract = interactionEnabled,
                canSubmitVote =
                    interactionEnabled &&
                        validDraft.isNotEmpty() &&
                        validDraft != submittedOptionIds,
                isChangingVote = submittedOptionIds.isNotEmpty(),
                canShowVotes = showVotes
            )

        return normalizedState.copy(
            votersOverlay =
                if (votersOverlay != null && showVotes) {
                    normalizedState.toVotersUiState()
                } else {
                    null
                }
        )
    }

    private fun PollMessageUiState.toVotersUiState(): PollVotersUiState =
        PollVotersUiState(
            pollId = pollId,
            question = question,
            totalVoters = totalVoters,
            sections =
                options.map { option ->
                    PollVoterSectionUi(
                        optionId = option.id,
                        optionText = option.text,
                        voteCount = option.voteCount,
                        percentage = option.percentage,
                        voters = option.voters
                    )
                }
        )
}
