package com.cbgm.securechat.feature.chats.presentation.component.groupdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.model.GroupVerificationSummaryUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_details_accepted
import com.cbgm.securechat.resources.feature_chats_group_details_description
import com.cbgm.securechat.resources.feature_chats_group_details_total
import com.cbgm.securechat.resources.feature_chats_group_details_verified
import com.cbgm.securechat.resources.feature_chats_group_orphaned_description
import com.cbgm.securechat.resources.feature_chats_group_orphaned_title
import com.cbgm.securechat.resources.feature_chats_group_verification_pending_note
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupDetailsSummary(summary: GroupVerificationSummaryUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.feature_chats_group_details_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (summary.isOrphaned) {
            Text(
                text = stringResource(Res.string.feature_chats_group_orphaned_title),
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_orphaned_description),
                modifier = Modifier.padding(top = MaterialTheme.spacing.base.div(2)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            GroupDetailMetric(
                value = summary.mutuallyVerifiedParticipantCount,
                label = stringResource(Res.string.feature_chats_group_details_verified),
                modifier = Modifier.weight(1f)
            )
            GroupDetailMetric(
                value = summary.activeParticipantCount,
                label = stringResource(Res.string.feature_chats_group_details_accepted),
                modifier = Modifier.weight(1f)
            )
            GroupDetailMetric(
                value = summary.totalMemberCount,
                label = stringResource(Res.string.feature_chats_group_details_total),
                modifier = Modifier.weight(1f)
            )
        }

        if (summary.members.any { member -> !member.isActive }) {
            Text(
                text = stringResource(Res.string.feature_chats_group_verification_pending_note),
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun GroupDetailsSummaryPreview() {
    SecureChatTheme {
        GroupDetailsSummary(summary = GroupDetailsPreviewData.summary)
    }
}
