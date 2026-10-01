package com.cbgm.sparrow.feature.polls.presentation.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.polls.presentation.message.component.PollMessageMedia
import com.cbgm.sparrow.feature.polls.presentation.message.component.PollOptionResult
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollMessageUiState
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollOptionUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_change_vote
import com.cbgm.sparrow.resources.feature_polls_close_poll
import com.cbgm.sparrow.resources.feature_polls_closed
import com.cbgm.sparrow.resources.feature_polls_expired
import com.cbgm.sparrow.resources.feature_polls_multiple_answers
import com.cbgm.sparrow.resources.feature_polls_vote
import com.cbgm.sparrow.resources.feature_polls_voters
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PollMessageContent(
    initialState: PollMessageUiState,
    onVoteSubmit: (Set<String>) -> Unit,
    onClosePoll: () -> Unit,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel =
        koinViewModel<PollMessageViewModel>(key = "poll:${initialState.pollId}") {
            parametersOf(initialState)
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PollMessageContentBody(
        uiState = uiState,
        onOptionClick = viewModel::onOptionClick,
        onVoteSubmit = { viewModel.submitVote(onVoteSubmit) },
        onClosePoll = onClosePoll,
        onMediaClick = onMediaClick,
        modifier = modifier
    )
}

@Composable
private fun PollMessageContentBody(
    uiState: PollMessageUiState,
    onOptionClick: (String) -> Unit,
    onVoteSubmit: () -> Unit,
    onClosePoll: () -> Unit,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
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

        PollMessageMedia(media = uiState.media, onMediaClick = onMediaClick)

        uiState.options.forEach { option ->
            PollOptionResult(
                text = option.text,
                voteCount = option.voteCount,
                percentage = option.percentage,
                selected = option.isSelected,
                enabled = uiState.canInteract,
                onClick = { onOptionClick(option.id) }
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.feature_polls_voters,
                        uiState.totalVoters,
                        uiState.totalVoters
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.allowMultipleSelection) {
                    Text(
                        text = stringResource(Res.string.feature_polls_multiple_answers),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                uiState.expiryLabel?.let { expiry ->
                    Text(
                        text = expiry,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                when {
                    uiState.isClosed -> PollStateLabel(stringResource(Res.string.feature_polls_closed))
                    uiState.isExpired -> PollStateLabel(stringResource(Res.string.feature_polls_expired))
                }
            }

            Column {
                if (uiState.canSubmitVote) {
                    TextButton(onClick = onVoteSubmit) {
                        Text(
                            text = stringResource(
                                if (uiState.isChangingVote) {
                                    Res.string.feature_polls_change_vote
                                } else {
                                    Res.string.feature_polls_vote
                                }
                            )
                        )
                    }
                }
                if (uiState.canClose && !uiState.isClosed) {
                    TextButton(onClick = onClosePoll) {
                        Text(text = stringResource(Res.string.feature_polls_close_poll))
                    }
                }
            }
        }
    }
}

@Composable
private fun PollStateLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
}

private fun previewState(
    mediaCount: Int = 0,
    allowMultiple: Boolean = false,
    submitted: Set<String> = emptySet(),
    draft: Set<String> = submitted,
    expired: Boolean = false,
    closed: Boolean = false
) = PollMessageUiState(
    pollId = "preview",
    question = "What should we do this weekend?",
    description = "Let's decide together!",
    media = List(mediaCount) { index ->
        MediaItemUi(
            id = "media-$index",
            type = MediaTypeUi.IMAGE,
            mimeType = "image/jpeg",
            localFilePath = "/preview/$index.jpg"
        )
    },
    options = listOf(
        PollOptionUi("1", "Go hiking", 12, 50),
        PollOptionUi("2", "Visit a city", 6, 25),
        PollOptionUi("3", "Stay at home", 4, 17)
    ),
    totalVoters = 24,
    submittedOptionIds = submitted,
    draftOptionIds = draft,
    allowMultipleSelection = allowMultiple,
    allowVoteChange = true,
    isExpired = expired,
    isClosed = closed,
    canInteract = !expired && !closed,
    canSubmitVote = !expired && !closed && draft.isNotEmpty() && draft != submitted,
    isChangingVote = submitted.isNotEmpty(),
    expiryLabel = if (expired) "Poll ended 25 May 2026 at 18:00" else "Poll ends 1 Oct 2026 at 18:00",
    canClose = !closed
)

@Preview
@Composable
private fun PollMessageNotVotedPreview() {
    SparrowTheme { Surface { PollMessageContentBody(previewState(), {}, {}, {}, {}) } }
}

@Preview
@Composable
private fun PollMessageVotedPreview() {
    SparrowTheme { Surface { PollMessageContentBody(previewState(submitted = setOf("1")), {}, {}, {}, {}) } }
}

@Preview
@Composable
private fun PollMessageMultipleDraftPreview() {
    SparrowTheme { Surface { PollMessageContentBody(previewState(allowMultiple = true, draft = setOf("1", "2")), {}, {}, {}, {}) } }
}

@Preview
@Composable
private fun PollMessageMediaOverflowPreview() {
    SparrowTheme { Surface { PollMessageContentBody(previewState(mediaCount = 5), {}, {}, {}, {}) } }
}

@Preview
@Composable
private fun PollMessageExpiredPreview() {
    SparrowTheme { Surface { PollMessageContentBody(previewState(expired = true), {}, {}, {}, {}) } }
}

@Preview
@Composable
private fun PollMessageClosedPreview() {
    SparrowTheme { Surface { PollMessageContentBody(previewState(closed = true), {}, {}, {}, {}) } }
}
