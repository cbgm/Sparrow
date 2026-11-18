package com.cbgm.sparrow.feature.polls.presentation.message.component

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cbgm.sparrow.core.ui.component.PercentageBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.polls.presentation.model.PollVoterUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_voters
import org.jetbrains.compose.resources.pluralStringResource

@Composable
fun PollOptionResult(
    text: String,
    voteCount: Int,
    percentage: Int,
    voters: List<PollVoterUi>,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.micro),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
        ) {
            if (selected) {
                Icon(
                    imageVector = SparrowIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.Poll.checkIconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            PollVoterPreview(voters = voters)
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PercentageBar(
            percentage = percentage,
            modifier = Modifier.fillMaxWidth(),
            height = Dimens.Poll.barHeight,
            trackColor = MaterialTheme.colorScheme.divider,
            progressColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )

        Text(
            text = pluralStringResource(Res.plurals.feature_polls_voters, voteCount, voteCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                name = voter.displayName,
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
                    onClick = {}
                )
            }
        }
    }
}
