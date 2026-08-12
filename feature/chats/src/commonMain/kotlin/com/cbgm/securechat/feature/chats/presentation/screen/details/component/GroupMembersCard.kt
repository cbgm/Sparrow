package com.cbgm.securechat.feature.chats.presentation.component.groupdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.model.GroupVerificationSummaryUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_details_members
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupMembersCard(
    summary: GroupVerificationSummaryUiState,
    onVerifyMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteMember: (String) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.feature_chats_group_details_members),
            modifier =
                Modifier.padding(
                    start = MaterialTheme.spacing.small,
                    bottom = MaterialTheme.spacing.small
                ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Column {
                summary.members.forEachIndexed { index, member ->
                    GroupMemberRow(
                        member = member,
                        showVerifyAction = summary.isLocalAdmin && member.canVerify,
                        showRemoveAction =
                            summary.isLocalAdmin &&
                                !member.isGroupAdmin &&
                                member.contactId != null,
                        showPromoteAction =
                            summary.isLocalAdmin &&
                                member.contactId in summary.promotableContactIds,
                        showDivider = index < summary.members.lastIndex,
                        onVerify = {
                            member.contactId?.let(onVerifyMember)
                        },
                        onRemove = {
                            member.contactId?.let(onRemoveMember)
                        },
                        onPromote = {
                            member.contactId?.let(onPromoteMember)
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun GroupMembersCardPreview() {
    SecureChatTheme {
        GroupMembersCard(
            summary = GroupDetailsPreviewData.summary,
            onVerifyMember = {},
            onRemoveMember = {},
            onPromoteMember = {}
        )
    }
}
