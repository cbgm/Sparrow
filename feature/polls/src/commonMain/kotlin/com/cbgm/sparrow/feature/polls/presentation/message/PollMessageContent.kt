package com.cbgm.sparrow.feature.polls.presentation.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.polls.presentation.component.pollDurationLabel
import com.cbgm.sparrow.feature.polls.presentation.message.component.PollMessageMedia
import com.cbgm.sparrow.feature.polls.presentation.message.component.PollOptionResult
import com.cbgm.sparrow.feature.polls.presentation.message.mapper.toPollMessageUiState
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollMessageUiState
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollOptionUi
import com.cbgm.sparrow.feature.polls.presentation.model.PollVoterUi
import com.cbgm.sparrow.feature.polls.presentation.voters.PollVotersScreen
import com.cbgm.sparrow.feature.polls.presentation.voters.model.PollVotersUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_anonymous
import com.cbgm.sparrow.resources.feature_polls_close_poll
import com.cbgm.sparrow.resources.feature_polls_closed
import com.cbgm.sparrow.resources.feature_polls_multiple_answers
import com.cbgm.sparrow.resources.feature_polls_open_for
import com.cbgm.sparrow.resources.feature_polls_show_votes
import com.cbgm.sparrow.resources.feature_polls_voters
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PollMessageContent(
    part: PollUi,
    color: Color,
    onVoteSubmit: (Set<String>) -> Unit,
    onClosePoll: () -> Unit,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<PollMessageViewModel>(key = part.instanceKey)
    val isVotersOverlayVisible by viewModel.isVotersOverlayVisible.collectAsStateWithLifecycle()
    val nowEpochMilliseconds = rememberPollNowEpochMilliseconds(part)
    val uiState =
        part.toPollMessageUiState(nowEpochMilliseconds).copy(
            isVotersOverlayVisible = isVotersOverlayVisible
        )

    PollMessageContentBody(
        uiState = uiState,
        color = color,
        onOptionClick = { optionId ->
            if (uiState.canInteract && uiState.options.any { it.id == optionId }) {
                val selectedOptionIds =
                    if (uiState.allowMultipleSelection) {
                        if (optionId in uiState.submittedOptionIds) {
                            uiState.submittedOptionIds - optionId
                        } else {
                            uiState.submittedOptionIds + optionId
                        }
                    } else {
                        setOf(optionId)
                    }

                if (selectedOptionIds.isNotEmpty() && selectedOptionIds != uiState.submittedOptionIds) {
                    onVoteSubmit(selectedOptionIds)
                }
            }
        },
        onClosePoll = onClosePoll,
        onMediaClick = onMediaClick,
        onShowVotes = viewModel::openVoters,
        modifier = modifier
    )

    PollVotersOverlay(
        state = uiState.votersOverlay.takeIf { uiState.isVotersOverlayVisible },
        onDismissRequest = viewModel::dismissVoters
    )
}

@Composable
private fun PollVotersOverlay(
    state: PollVotersUiState?,
    onDismissRequest: () -> Unit
) {
    SparrowOverlayHost(
        visible = state != null,
        onDismissRequest = onDismissRequest,
        horizontalPadding = MaterialTheme.spacing.zero,
        topPadding = MaterialTheme.spacing.times(6)
    ) { dismissOverlay ->
        state?.let { voters ->
            PollVotersScreen(
                uiState = voters,
                onClose = dismissOverlay,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PollMessageContentBody(
    uiState: PollMessageUiState,
    onOptionClick: (String) -> Unit,
    onClosePoll: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onShowVotes: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Text(
            text = uiState.question,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        uiState.description?.takeIf(String::isNotBlank)?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PollMessageMedia(
            media = uiState.mediaPreview,
            remainingCount = uiState.remainingMediaCount,
            onMediaClick = onMediaClick
        )

        uiState.options.forEach { option ->
            PollOptionResult(
                color = color,
                text = option.text,
                voteCount = option.voteCount,
                percentage = option.percentage,
                voters = option.voterPreview,
                selected = option.isSelected && !uiState.isClosed,
                enabled = uiState.canInteract,
                onClick = { onOptionClick(option.id) }
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                ) {
                    Text(
                        text = pluralStringResource(
                            Res.plurals.feature_polls_voters,
                            uiState.totalVoters,
                            uiState.totalVoters
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.canShowVotes) {
                        Text(
                            text = stringResource(Res.string.feature_polls_show_votes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = onShowVotes
                                ).padding(MaterialTheme.spacing.base)
                        )
                    }
                }
                if (uiState.allowMultipleSelection) {
                    Text(
                        text = stringResource(Res.string.feature_polls_multiple_answers),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (uiState.isAnonymous) {
                    Text(
                        text = stringResource(Res.string.feature_polls_anonymous),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                uiState.remainingOpenMinutes?.let { minutes ->
                    Text(
                        text = stringResource(
                            Res.string.feature_polls_open_for,
                            pollDurationLabel(minutes)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onClosePoll,
                enabled = !uiState.isClosed && uiState.canClose,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .size(Dimens.Poll.buttonSize)
            ) {
                Icon(
                    imageVector = if (uiState.isClosed) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription =
                        stringResource(
                            if (uiState.isClosed) {
                                Res.string.feature_polls_closed
                            } else {
                                Res.string.feature_polls_close_poll
                            }
                        ),
                    tint =
                        if (uiState.isClosed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            if (uiState.canClose) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        }
                )
            }
        }
    }
}

private fun previewState(
    mediaCount: Int = 0,
    allowMultiple: Boolean = false,
    submitted: Set<String> = emptySet(),
    closed: Boolean = false,
    anonymous: Boolean = false
): PollMessageUiState {
    val media = List(mediaCount) { index ->
        MediaItemUi(
            id = "media-$index",
            type = MediaTypeUi.IMAGE,
            mimeType = "image/jpeg",
            localFilePath = "/preview/$index.jpg"
        )
    }
    val firstOptionVoters =
        listOf(
            PollVoterUi("alice", "Alice"),
            PollVoterUi("bob", "Bob"),
            PollVoterUi("chris", "Chris"),
            PollVoterUi("dana", "Dana")
        )
    val secondOptionVoters = listOf(PollVoterUi("erin", "Erin"), PollVoterUi("frank", "Frank"))
    val thirdOptionVoters = listOf(PollVoterUi("grace", "Grace"))

    return PollMessageUiState(
        pollId = "preview",
        question = "What should we do this weekend?",
        description = "Let's decide together!",
        media = media,
        mediaPreview = media.take(3),
        remainingMediaCount = (media.size - 3).coerceAtLeast(0),
        options = listOf(
            PollOptionUi(
                id = "1",
                text = "Go hiking",
                voteCount = 12,
                voters = if (anonymous) emptyList() else firstOptionVoters,
                voterPreview = if (anonymous) emptyList() else firstOptionVoters.take(3),
                percentage = 50
            ),
            PollOptionUi(
                id = "2",
                text = "Visit a city",
                voteCount = 6,
                voters = if (anonymous) emptyList() else secondOptionVoters,
                voterPreview = if (anonymous) emptyList() else secondOptionVoters,
                percentage = 25
            ),
            PollOptionUi(
                id = "3",
                text = "Stay at home",
                voteCount = 4,
                voters = if (anonymous) emptyList() else thirdOptionVoters,
                voterPreview = if (anonymous) emptyList() else thirdOptionVoters,
                percentage = 17
            )
        ),
        totalVoters = 24,
        submittedOptionIds = submitted,
        allowMultipleSelection = allowMultiple,
        allowVoteChange = true,
        isAnonymous = anonymous,
        isClosed = closed,
        canInteract = !closed,
        canShowVotes = !anonymous,
        remainingOpenMinutes = if (closed) null else 3_600L,
        canClose = true
    )
}

@Preview
@Composable
private fun PollMessageNotVotedPreview() {
    SparrowTheme { Surface { PollMessageContentBody(previewState(), {}, {}, {}, {}) } }
}

@Preview
@Composable
private fun PollMessageVotedPreview() {
    SparrowTheme {
        Surface {
            PollMessageContentBody(
                previewState(submitted = setOf("1")),
                {},
                {},
                {},
                {}
            )
        }
    }
}

@Preview
@Composable
private fun PollMessageMultipleSelectionPreview() {
    SparrowTheme {
        Surface {
            PollMessageContentBody(
                previewState(allowMultiple = true, submitted = setOf("1", "2")),
                {},
                {},
                {},
                {}
            )
        }
    }
}

@Preview
@Composable
private fun PollMessageMediaOverflowPreview() {
    SparrowTheme {
        Surface {
            PollMessageContentBody(
                previewState(mediaCount = 5),
                {},
                {},
                {},
                {}
            )
        }
    }
}

@Preview
@Composable
private fun PollMessageClosedPreview() {
    SparrowTheme { Surface { PollMessageContentBody(previewState(closed = true), {}, {}, {}, {}) } }
}

@Preview
@Composable
private fun PollMessageAnonymousPreview() {
    SparrowTheme {
        Surface {
            PollMessageContentBody(
                previewState(anonymous = true),
                {},
                {},
                {},
                {}
            )
        }
    }
}
