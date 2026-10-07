package com.cbgm.sparrow.feature.polls.presentation.voters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.polls.presentation.model.PollVoterUi
import com.cbgm.sparrow.feature.polls.presentation.voters.model.PollVoterSectionUi
import com.cbgm.sparrow.feature.polls.presentation.voters.model.PollVotersUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_polls_no_votes
import com.cbgm.sparrow.resources.feature_polls_percentage
import com.cbgm.sparrow.resources.feature_polls_voters
import com.cbgm.sparrow.resources.feature_polls_voters_title
import com.cbgm.sparrow.resources.feature_polls_you
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PollVotersScreen(
    uiState: PollVotersUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            PollVotersTopBar(
                containerColor = containerColor,
                onClose = onClose
            )
        }
    ) { innerPadding, listState ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            item(key = "poll-voters-header") {
                PollVotersHeader(
                    uiState = uiState,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding)
                )
            }

            items(
                items = uiState.sections,
                key = PollVoterSectionUi::optionId
            ) { section ->
                PollVoterGroup(
                    section = section,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding)
                )
            }
        }
    }
}

@Composable
private fun PollVotersHeader(
    uiState: PollVotersUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Text(
            text = uiState.question,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = pluralStringResource(
                Res.plurals.feature_polls_voters,
                uiState.totalVoters,
                uiState.totalVoters
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PollVoterGroup(
    section: PollVoterSectionUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = section.optionText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = pluralStringResource(
                    Res.plurals.feature_polls_voters,
                    section.voteCount,
                    section.voteCount
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.feature_polls_percentage, section.percentage),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = Dimens.Base.zero,
            shadowElevation = Dimens.Base.zero
        ) {
            if (section.voters.isEmpty()) {
                Text(
                    text = stringResource(Res.string.feature_polls_no_votes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.base),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column {
                    section.voters.forEachIndexed { index, voter ->
                        PollVoterRow(
                            voter = voter,
                            showDivider = index < section.voters.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PollVoterRow(
    voter: PollVoterUi,
    showDivider: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            leadingContent = {
                SparrowAvatar(
                    name = voter.displayName(),
                    target = AvatarTarget.User(voter.id)
                )
            },
            headlineContent = {
                Text(
                    text = voter.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = MaterialTheme.spacing.times(8)),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun PollVoterUi.displayName(): String =
    if (id == PollPolicy.LOCAL_VOTER_ID) {
        stringResource(Res.string.feature_polls_you)
    } else {
        displayName
    }

@Composable
private fun PollVotersTopBar(
    containerColor: Color,
    onClose: () -> Unit
) {
    CenterAlignedTopAppBar(
        windowInsets = WindowInsets(MaterialTheme.spacing.zero),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Text(
                text = stringResource(Res.string.feature_polls_voters_title),
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.base_close)
                )
            }
        }
    )
}

@Preview(heightDp = 760)
@Composable
private fun PollVotersScreenPreview() {
    SparrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PollVotersScreen(
                uiState = PollVotersUiState(
                    pollId = "poll-1",
                    question = "What should we do this weekend?",
                    totalVoters = 7,
                    sections = listOf(
                        PollVoterSectionUi(
                            optionId = "option-1",
                            optionText = "Go hiking",
                            voteCount = 4,
                            percentage = 57,
                            voters = listOf(
                                PollVoterUi("alice", "Alice"),
                                PollVoterUi("bob", "Bob"),
                                PollVoterUi("chris", "Chris"),
                                PollVoterUi("dana", "Dana")
                            )
                        ),
                        PollVoterSectionUi(
                            optionId = "option-2",
                            optionText = "Visit a city",
                            voteCount = 2,
                            percentage = 29,
                            voters = listOf(
                                PollVoterUi("emma", "Emma"),
                                PollVoterUi("frank", "Frank")
                            )
                        ),
                        PollVoterSectionUi(
                            optionId = "option-3",
                            optionText = "Stay at home",
                            voteCount = 1,
                            percentage = 14,
                            voters = listOf(PollVoterUi("grace", "Grace"))
                        )
                    )
                ),
                onClose = {}
            )
        }
    }
}
