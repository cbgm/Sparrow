package com.cbgm.sparrow.feature.polls.presentation.message.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cbgm.sparrow.core.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.core.ui.component.PercentageBar
import com.cbgm.sparrow.core.ui.helper.darker
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.polls.presentation.model.PollVoterUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_voters
import com.cbgm.sparrow.resources.feature_polls_you
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PollOptionResult(
    text: String,
    voteCount: Int,
    percentage: Int,
    voters: List<PollVoterUi>,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = color.darker(0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border =
            BorderStroke(
                width = Dimens.Base.borderStrokeWidth,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
            )
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.spacing.medium),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PercentageBar(
                percentage = percentage,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
            ) {
                PollVoterPreview(voters = voters)

                Text(
                    text =
                        pluralStringResource(
                            Res.plurals.feature_polls_voters,
                            voteCount,
                            voteCount
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PollVoterPreview(voters: List<PollVoterUi>) {
    if (voters.isEmpty()) return

    val avatarSize = MaterialTheme.spacing.medium
    val overlap = MaterialTheme.spacing.micro
    val step = avatarSize - overlap
    val previewWidth = avatarSize + (step * (voters.size - 1))

    Box(
        modifier =
            Modifier
                .width(previewWidth)
                .height(avatarSize)
    ) {
        voters.forEachIndexed { index, voter ->
            SparrowAvatar(
                name = voter.displayName(),
                target = AvatarTarget.User(voter.id),
                size = avatarSize,
                modifier =
                    Modifier
                        .offset(x = step * index)
                        .zIndex(index.toFloat())
                        .border(
                            width = Dimens.Base.borderStrokeWidth,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.circle
                        )
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

@Preview
@Composable
private fun PollOptionResultPreview() {
    SparrowTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.base),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                PollOptionResult(
                    text = "Go hiking",
                    voteCount = 12,
                    percentage = 50,
                    voters =
                        listOf(
                            PollVoterUi("alice", "Alice"),
                            PollVoterUi("bob", "Bob"),
                            PollVoterUi("chris", "Chris")
                        ),
                    selected = false,
                    enabled = true,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    onClick = {}
                )
                PollOptionResult(
                    text = "Visit a city",
                    voteCount = 6,
                    percentage = 25,
                    voters =
                        listOf(
                            PollVoterUi("dana", "Dana"),
                            PollVoterUi("erin", "Erin")
                        ),
                    selected = true,
                    enabled = true,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    onClick = {}
                )
            }
        }
    }
}
